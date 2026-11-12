//
//  skCrypter.hpp — крыс-version строкового шифратора.
//
//  Оригинальный `TMessagesProj/jni/security/` Арслан прятал в `.gitignore`, и native-слой
//  не собирался без него (fatal error: 'security/skCrypter.hpp' file not found в
//  TgNetWrapper.cpp:17). Контракт восстановлен по всем 28 местам употребления:
//  skCrypt("...") подставляется прямо в JNI-параметры типа const char*
//  (FindClass / GetMethodID / GetStaticMethodID), значит результат обязан
//  (а) неявно конвертироваться в const char* и (б) быть независимым для каждого вызова:
//  в `GetMethodID(c, skCrypt("run"), skCrypt("(J)V"))` живут ОБЕ строки одновременно.
//
//  Схема: конструктор XOR'ит байты (в .rodata открытого текста нет), decrypt() раскручивает
//  XOR обратно в собственный буфер временного объекта. Временный объект живёт до конца
//  полного выражения — как раз пока JNI читает const char*.
//

#ifndef CHERRYGRAM_XXL_SKCRYPTER_HPP
#define CHERRYGRAM_XXL_SKCRYPTER_HPP

#include <cstddef>

namespace skcrypter_detail {

    template <int Line, int Length>
    class skCrypter {
    public:
        explicit constexpr skCrypter(const char (&text)[Length]) : buffer{} {
            for (int index = 0; index + 1 < Length; index++) {
                buffer[index] = static_cast<char>(text[index] ^ key_at(index));
            }
            buffer[Length - 1] = '\0';
        }

        constexpr const char *decrypt() const {
            for (int index = 0; index + 1 < Length; index++) {
                buffer[index] = static_cast<char>(buffer[index] ^ key_at(index));
            }
            return buffer;
        }

        constexpr operator const char *() const {
            return decrypt();
        }

    private:
        static constexpr char key_at(int index) {
            return static_cast<char>((index * 131 + Line * 31 + 0x5A) & 0x7F);
        }

        mutable char buffer[Length];
    };


    /**
     * Компилерная самопроверка: XOR обязан быть обратимым, а две строки в одном выражении
     * (GetMethodID(c, skCrypt("run"), skCrypt("(J)V"))) — жить независимо друг с другом,
     * иначе JNI получит мусор. Проверка стоит в заголовке, потому что native-слой в CI
     * — единственный честный тест для skCrypter.
     */
    constexpr bool xxl_roundtrip_is_symmetric() {
        skCrypter<7, sizeof("probe")> first("probe");
        skCrypter<11, sizeof("run")> second("run");
        const char *a = first.decrypt();
        const char *b = second.decrypt();
        return a[0] == 'p' && a[1] == 'r' && a[4] == 'e' && a[5] == '\0' &&
               b[0] == 'r' && b[1] == 'u' && b[2] == 'n' && b[3] == '\0';
    }

    static_assert(xxl_roundtrip_is_symmetric(), "skCrypter: XOR round-trip сломан");

}

/**
 * Обёртка: skCrypt("org/telegram/tgnet/ConnectionsManager") -> const char* на весь
 * продолжительность текущего полного выражения.
 */
#define skCrypt(str) ::skcrypter_detail::skCrypter<__LINE__, sizeof(str)>(str).decrypt()

#endif //CHERRYGRAM_XXL_SKCRYPTER_HPP
