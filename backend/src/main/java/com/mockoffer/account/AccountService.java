package com.mockoffer.account;

import com.mockoffer.account.domain.Provider;
import com.mockoffer.account.domain.User;
import com.mockoffer.account.domain.UserIdentity;
import com.mockoffer.account.repo.UserIdentityRepository;
import com.mockoffer.account.repo.UserRepository;
import com.mockoffer.common.BizException;
import com.mockoffer.common.Emails;
import com.mockoffer.common.Snowflake;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 账号归一与登录方式管理：永不跨方式自动归并；绑定占用即拒；解绑「至少保留一种」。 */
@Service
public class AccountService {

    private final UserRepository users;
    private final UserIdentityRepository identities;
    private final Snowflake snowflake;
    private final ObjectMapper objectMapper;

    public AccountService(UserRepository users, UserIdentityRepository identities, Snowflake snowflake,
            ObjectMapper objectMapper) {
        this.users = users;
        this.identities = identities;
        this.snowflake = snowflake;
        this.objectMapper = objectMapper;
    }

    /** 展示用账号标识：邮箱给邮箱地址，GitHub 给 @用户名，其余为空。两个接口共用此逻辑。 */
    public String accountLabel(UserIdentity i) {
        if (Provider.EMAIL_OTP.equals(i.getProvider())) {
            return i.getProviderUid();
        }
        if (Provider.GITHUB.equals(i.getProvider()) && i.getMeta() != null) {
            try {
                JsonNode node = objectMapper.readTree(i.getMeta());
                return node.hasNonNull("login") ? "@" + node.get("login").asText() : null;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /** 按 (provider, providerUid) 命中则进对应账号，否则新建账号 + 身份。 */
    @Transactional
    public long loginOrRegister(String provider, String providerUid, String metaJson) {
        OffsetDateTime now = OffsetDateTime.now();
        var existing = identities.findByProviderAndProviderUidAndDeletedAtIsNull(provider, providerUid);

        User user;
        if (existing.isPresent()) {
            UserIdentity identity = existing.get();
            user = users.findById(identity.getUserId())
                    .orElseThrow(() -> new BizException(50002, "账号数据异常"));
            if (!user.isActive()) {
                throw new BizException(40301, "账号已被禁用");
            }
            identity.setLastUsedAt(now);
            if (metaJson != null) {
                identity.setMeta(metaJson);
            }
            identities.save(identity);
        } else {
            user = new User();
            users.save(user);
            identities.save(newIdentity(user.getId(), provider, providerUid, metaJson, now));
        }
        user.setLastLoginAt(now);
        users.save(user);
        return user.getId();
    }

    /** 绑定第三方到指定账号：目标身份被占用则拒绝；已绑到自己则幂等返回。 */
    @Transactional
    public void bindIdentity(long userId, String provider, String providerUid, String metaJson) {
        var existing = identities.findByProviderAndProviderUidAndDeletedAtIsNull(provider, providerUid);
        if (existing.isPresent()) {
            if (existing.get().getUserId() == userId) {
                return;
            }
            throw new BizException(40901, "该登录方式已绑定其他账号");
        }
        identities.save(newIdentity(userId, provider, providerUid, metaJson, OffsetDateTime.now()));
    }

    public List<UserIdentity> listIdentities(long userId) {
        return identities.findByUserIdAndDeletedAtIsNull(userId);
    }

    /** 解绑：仅剩一种时拒绝；软删后二次校验兜底并发。 */
    @Transactional
    public void unbind(long userId, String provider) {
        var opt = identities.findByUserIdAndProviderAndDeletedAtIsNull(userId, provider);
        if (opt.isEmpty()) {
            throw new BizException(40401, "未绑定该登录方式");
        }
        if (identities.countByUserIdAndDeletedAtIsNull(userId) <= 1) {
            throw new BizException(40902, "至少保留一种登录方式");
        }
        UserIdentity identity = opt.get();
        identity.setDeletedAt(OffsetDateTime.now());
        identities.saveAndFlush(identity);
        if (identities.countByUserIdAndDeletedAtIsNull(userId) < 1) {
            throw new BizException(40902, "至少保留一种登录方式");
        }
    }

    /** 换绑 / 绑定邮箱：新邮箱被他人占用则拒；有 email_otp 身份则改其 uid，否则新增。 */
    @Transactional
    public void changeEmail(long userId, String newEmailRaw) {
        String email = Emails.normalize(newEmailRaw);
        var occupied = identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.EMAIL_OTP, email);
        if (occupied.isPresent() && occupied.get().getUserId() != userId) {
            throw new BizException(40901, "该邮箱已绑定其他账号");
        }
        OffsetDateTime now = OffsetDateTime.now();
        var mine = identities.findByUserIdAndProviderAndDeletedAtIsNull(userId, Provider.EMAIL_OTP);
        if (mine.isPresent()) {
            UserIdentity identity = mine.get();
            identity.setProviderUid(email);
            identity.setLastUsedAt(now);
            identities.save(identity);
        } else {
            identities.save(newIdentity(userId, Provider.EMAIL_OTP, email, null, now));
        }
    }

    private UserIdentity newIdentity(long userId, String provider, String providerUid, String meta, OffsetDateTime now) {
        UserIdentity identity = new UserIdentity();
        identity.setId(snowflake.nextId());
        identity.setUserId(userId);
        identity.setProvider(provider);
        identity.setProviderUid(providerUid);
        identity.setMeta(meta);
        identity.setLastUsedAt(now);
        return identity;
    }
}
