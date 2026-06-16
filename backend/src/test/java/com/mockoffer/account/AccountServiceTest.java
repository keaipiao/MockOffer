package com.mockoffer.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mockoffer.account.domain.Provider;
import com.mockoffer.account.domain.User;
import com.mockoffer.account.domain.UserIdentity;
import com.mockoffer.account.repo.UserIdentityRepository;
import com.mockoffer.account.repo.UserRepository;
import com.mockoffer.common.BizException;
import com.mockoffer.common.Snowflake;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

/** AccountService 核心业务单测：账号归一、绑定、解绑、换绑邮箱。纯 Mockito，不依赖 DB。 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock UserRepository users;
    @Mock UserIdentityRepository identities;
    @Mock Snowflake snowflake;
    @Mock ObjectMapper objectMapper;

    @InjectMocks AccountService account;

    // ---------- loginOrRegister ----------

    @Test
    void 命中身份且账号有效_进同账号并更新使用痕迹() {
        User user = activeUser(1L);
        UserIdentity identity = identity(1L, Provider.GITHUB, "gh1");
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.GITHUB, "gh1"))
                .thenReturn(Optional.of(identity));
        when(users.findById(1L)).thenReturn(Optional.of(user));

        long uid = account.loginOrRegister(Provider.GITHUB, "gh1", "{\"login\":\"a\"}");

        assertThat(uid).isEqualTo(1L);
        assertThat(identity.getMeta()).isEqualTo("{\"login\":\"a\"}");
        assertThat(identity.getLastUsedAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(identities).save(identity);
        verify(users).save(user);
    }

    @Test
    void 命中身份但账号被禁用_拒绝40301() {
        User disabled = activeUser(1L);
        disabled.setStatus(User.STATUS_DISABLED);
        UserIdentity identity = identity(1L, Provider.GITHUB, "gh1");
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.GITHUB, "gh1"))
                .thenReturn(Optional.of(identity));
        when(users.findById(1L)).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> account.loginOrRegister(Provider.GITHUB, "gh1", null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(40301));
    }

    @Test
    void 未命中身份_新建账号与身份() {
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.EMAIL_OTP, "a@b.com"))
                .thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                ReflectionTestUtils.setField(u, "id", 100L);
            }
            return u;
        });
        when(snowflake.nextId()).thenReturn(5000L);

        long uid = account.loginOrRegister(Provider.EMAIL_OTP, "a@b.com", null);

        assertThat(uid).isEqualTo(100L);
        ArgumentCaptor<UserIdentity> saved = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identities).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo(Provider.EMAIL_OTP);
        assertThat(saved.getValue().getProviderUid()).isEqualTo("a@b.com");
        assertThat(saved.getValue().getUserId()).isEqualTo(100L);
    }

    // ---------- bindIdentity ----------

    @Test
    void 绑定目标已属于自己_幂等不重复落库() {
        UserIdentity mine = identity(5L, Provider.GITHUB, "gh1");
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.GITHUB, "gh1"))
                .thenReturn(Optional.of(mine));

        account.bindIdentity(5L, Provider.GITHUB, "gh1", "{}");

        verify(identities, never()).save(any());
    }

    @Test
    void 绑定目标已被他人占用_拒绝40901() {
        UserIdentity others = identity(6L, Provider.GITHUB, "gh1");
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.GITHUB, "gh1"))
                .thenReturn(Optional.of(others));

        BizException e = catchThrowableOfType(BizException.class,
                () -> account.bindIdentity(5L, Provider.GITHUB, "gh1", "{}"));
        assertThat(e.getCode()).isEqualTo(40901);
    }

    @Test
    void 绑定目标空闲_落库新身份() {
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.GITHUB, "gh1"))
                .thenReturn(Optional.empty());
        when(snowflake.nextId()).thenReturn(5001L);

        account.bindIdentity(5L, Provider.GITHUB, "gh1", "{\"login\":\"x\"}");

        ArgumentCaptor<UserIdentity> saved = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identities).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(5L);
        assertThat(saved.getValue().getProvider()).isEqualTo(Provider.GITHUB);
        assertThat(saved.getValue().getProviderUid()).isEqualTo("gh1");
        assertThat(saved.getValue().getMeta()).isEqualTo("{\"login\":\"x\"}");
    }

    // ---------- unbind ----------

    @Test
    void 解绑未绑定的方式_拒绝40401() {
        when(identities.findByUserIdAndProviderAndDeletedAtIsNull(5L, Provider.GITHUB))
                .thenReturn(Optional.empty());

        BizException e = catchThrowableOfType(BizException.class,
                () -> account.unbind(5L, Provider.GITHUB));
        assertThat(e.getCode()).isEqualTo(40401);
    }

    @Test
    void 解绑最后一种方式_拒绝40902() {
        when(identities.findByUserIdAndProviderAndDeletedAtIsNull(5L, Provider.GITHUB))
                .thenReturn(Optional.of(identity(5L, Provider.GITHUB, "gh1")));
        when(identities.countByUserIdAndDeletedAtIsNull(5L)).thenReturn(1L);

        BizException e = catchThrowableOfType(BizException.class,
                () -> account.unbind(5L, Provider.GITHUB));
        assertThat(e.getCode()).isEqualTo(40902);
        verify(identities, never()).save(any());
    }

    @Test
    void 解绑仍保留其他方式_软删该身份() {
        UserIdentity target = identity(5L, Provider.GITHUB, "gh1");
        when(identities.findByUserIdAndProviderAndDeletedAtIsNull(5L, Provider.GITHUB))
                .thenReturn(Optional.of(target));
        when(identities.countByUserIdAndDeletedAtIsNull(5L)).thenReturn(2L);

        account.unbind(5L, Provider.GITHUB);

        assertThat(target.getDeletedAt()).isNotNull();
        verify(identities).save(target);
    }

    // ---------- changeEmail ----------

    @Test
    void 换绑邮箱被他人占用_拒绝40901() {
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.EMAIL_OTP, "other@x.com"))
                .thenReturn(Optional.of(identity(9L, Provider.EMAIL_OTP, "other@x.com")));

        BizException e = catchThrowableOfType(BizException.class,
                () -> account.changeEmail(5L, "Other@X.com"));
        assertThat(e.getCode()).isEqualTo(40901);
    }

    @Test
    void 换绑邮箱已有邮箱身份_改其uid() {
        UserIdentity mine = identity(5L, Provider.EMAIL_OTP, "old@x.com");
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.EMAIL_OTP, "new@x.com"))
                .thenReturn(Optional.empty());
        when(identities.findByUserIdAndProviderAndDeletedAtIsNull(5L, Provider.EMAIL_OTP))
                .thenReturn(Optional.of(mine));

        account.changeEmail(5L, "new@x.com");

        assertThat(mine.getProviderUid()).isEqualTo("new@x.com");
        verify(identities).save(mine);
    }

    @Test
    void 绑定邮箱时无邮箱身份_新增email_otp身份() {
        when(identities.findByProviderAndProviderUidAndDeletedAtIsNull(Provider.EMAIL_OTP, "new@x.com"))
                .thenReturn(Optional.empty());
        when(identities.findByUserIdAndProviderAndDeletedAtIsNull(5L, Provider.EMAIL_OTP))
                .thenReturn(Optional.empty());
        when(snowflake.nextId()).thenReturn(5002L);

        account.changeEmail(5L, "new@x.com");

        ArgumentCaptor<UserIdentity> saved = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identities).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo(Provider.EMAIL_OTP);
        assertThat(saved.getValue().getProviderUid()).isEqualTo("new@x.com");
        assertThat(saved.getValue().getUserId()).isEqualTo(5L);
    }

    // ---------- helpers ----------

    private static User activeUser(long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static UserIdentity identity(long userId, String provider, String uid) {
        UserIdentity i = new UserIdentity();
        i.setUserId(userId);
        i.setProvider(provider);
        i.setProviderUid(uid);
        return i;
    }
}
