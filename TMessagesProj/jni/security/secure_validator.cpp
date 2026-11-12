//
//  secure_validator.cpp — крыс-version.
//
//  Контракт (3 символа из TgNetWrapper.cpp + 1 из закомментированной проверки при загрузке)
//  сохранён полностью, поведение — нейтральное: форк не выдаёт пользователя за то, что тот
//  поставил не ту подпись APK. См. комментарии в secure_validator.hpp.
//

#include "secure_validator.hpp"

namespace secure_validator {

    bool has_jni_hook(JNIEnv *env) {
        // Ни одна проверка не запускается: затхуканный JNI — легитимный способ починить клиент.
        return false;
    }

    bool has_xhook() {
        return false;
    }

    bool validate_signature(JNIEnv *env) {
        // Форк всегда подписан своим ключом (Your_Key.jks в CI), «несовпадение» — это и есть замысел.
        return true;
    }

    void maybeForceDisconnectOrUpdate(JNIEnv *env, int instanceNum, int reason) {
        // reason = 3 приходит после ConnectionsManager::init и после смены языка клиента.
        // Оригинальный код мог по этому сигналу разорвать соединение и разлогинить аккаунт.
        // Здесь тело пустое, поэтому ни разрыва, ни разлогина, ни ANR на попытке прочитать
        // подпись из натива не будет никогда.
        (void) env;
        (void) instanceNum;
        (void) reason;
    }

}
