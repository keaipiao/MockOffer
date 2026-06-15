package com.mockoffer.account.repo;

import com.mockoffer.account.domain.UserIdentity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    /** 登录归一键：按 (provider, provider_uid) 查未软删的身份。 */
    Optional<UserIdentity> findByProviderAndProviderUidAndDeletedAtIsNull(String provider, String providerUid);

    /** 列出某账号当前有效的登录方式。 */
    List<UserIdentity> findByUserIdAndDeletedAtIsNull(Long userId);

    /** 取某账号某 provider 的有效身份（解绑 / 换绑用）。 */
    Optional<UserIdentity> findByUserIdAndProviderAndDeletedAtIsNull(Long userId, String provider);

    /** 统计某账号有效登录方式数量（解绑「至少保留一种」校验用）。 */
    long countByUserIdAndDeletedAtIsNull(Long userId);
}
