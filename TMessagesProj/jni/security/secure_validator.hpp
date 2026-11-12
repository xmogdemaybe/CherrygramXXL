//
//  secure_validator.hpp — крыс-version.
//
//  Оригинальный `TMessagesProj/jni/security/` Арслан держал в `.gitignore`, поэтому из чистого
//  checkout нативный слой не конфигурировался (CMake Error at CMakeLists.txt:105: Cannot find
//  source file security/secure_validator.cpp). Здесь — рабочая реализация того же контракта,
//  который требует TgNetWrapper.cpp.
//
//  Назначение оригинала: найти фрейк/Frida/Xposed/переподписанный APK и в нужный момент
//  принудительно разорвать соединение и разлогинить аккаунт. В этом форке такое поведение —
//  ровно то, от чего форк существует (AGENTS.md §0.2: полный контроль клиента над собой),
//  поэтому валидатор всегда отвечает «всё чисто» и никогда никого не отключает.
//

#ifndef CHERRYGRAM_XXL_SECURE_VALIDATOR_HPP
#define CHERRYGRAM_XXL_SECURE_VALIDATOR_HPP

#include <jni.h>

namespace secure_validator {

    /** Признак того, что JNI-функции затхукали (Frida/inline-hook). Форк не сдаёт пользователей. */
    bool has_jni_hook(JNIEnv *env);

    /** Признак Xposed/LSPosed-хука. Всегда false: проверять чужий процесс — не задача крысы. */
    bool has_xhook();

    /** Совпадает ли подпись APK с ожидаемой. Форк подписан своим ключом — так и должно быть. */
    bool validate_signature(JNIEnv *env);

    /**
     * Вызывается из TgNetWrapper после init() и смены языка (reason = 3).
     * Оригинальная версия могла разорвать соединение/разлогинить аккаунт при подозрении на
     * взлом. Здесь это пустая функция: соединение рвёт сеть, а не идеология.
     */
    void maybeForceDisconnectOrUpdate(JNIEnv *env, int instanceNum, int reason);

}

#endif //CHERRYGRAM_XXL_SECURE_VALIDATOR_HPP
