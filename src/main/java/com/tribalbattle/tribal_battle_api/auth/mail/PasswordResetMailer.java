package com.tribalbattle.tribal_battle_api.auth.mail;

public interface PasswordResetMailer {

    void sendPasswordReset(
            String email,
            String displayName,
            String rawToken
    );
}
