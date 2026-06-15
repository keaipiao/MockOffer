-- M1 账号体系：用户主体 + 多登录身份
-- 全局约定：所有表带 deleted_at 软删；users.id 自增（社区可展示用户编号），其余表用雪花 ID（应用层生成）

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status        SMALLINT     NOT NULL DEFAULT 1,           -- 1 正常 / 0 禁用
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ
);
COMMENT ON TABLE users IS '账号主体（无密码、无 username）';

CREATE TABLE user_identities (
    id           BIGINT       PRIMARY KEY,                   -- 雪花 ID，应用层生成
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    provider     VARCHAR(20)  NOT NULL,                      -- email_otp / github /（预留）phone / wechat
    provider_uid VARCHAR(254) NOT NULL,                      -- 邮箱(规范化小写) / GitHub numeric id / 手机号 / unionid
    meta         JSONB,                                      -- provider 附加信息（含 GitHub 回传邮箱等），不参与唯一与归并
    last_used_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ
);
COMMENT ON TABLE user_identities IS '登录身份（邮箱 / GitHub，预留手机 / 微信）';

-- 部分唯一索引：仅对未软删行唯一（= 登录归一键 + 绑定占用判断；解绑软删后可重新绑定）
CREATE UNIQUE INDEX uk_provider_uid ON user_identities (provider, provider_uid) WHERE deleted_at IS NULL;
-- 按账号列其有效登录方式
CREATE INDEX idx_identity_user ON user_identities (user_id) WHERE deleted_at IS NULL;
