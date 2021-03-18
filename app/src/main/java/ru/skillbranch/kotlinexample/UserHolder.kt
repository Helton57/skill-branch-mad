package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extensions.checkPhoneNumber
import ru.skillbranch.kotlinexample.extensions.getCleanPhone

object UserHolder {
    private val map = mutableMapOf<String, User>()

    /**
     * Реализуй метод registerUser(fullName: String, email: String, password: String) возвращающий
     * объект User, если пользователь с таким же логином уже есть в системе необходимо бросить
     * исключение IllegalArgumentException("A user with this email already exists")
     */
    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName, email, password)
        .also { user ->
            if (map.containsKey(user.login)) {
                println("map.containsKey(user.login) user = ${user.login}")
                map.forEach { println(it) }
                throw IllegalArgumentException("A user with this email already exists")
            } else {
                println("map.containsKey(user.login) not user = ${user.login}")
                map[user.login] = user
                map.forEach { println(it) }
            }
        }

    /**
     * Реализуй метод registerUserByPhone(fullName: String, rawPhone: String) возвращающий объект User
     * (объект User должен содержать поле accessCode с 6 значным значением состоящим из случайных строчных и прописных букв латинского алфавита и цифр от 0 до 9),
     * если пользователь с таким же телефоном уже есть в системе необходимо бросить ошибку IllegalArgumentException("A user with this phone already exists")
     *
     * валидным является любой номер телефона содержащий первым символом + и 11 цифр и не содержащий буквы,
     * иначе необходимо бросить исключение IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
     */
    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        rawPhone.checkPhoneNumber()
        return User.makeUser(fullName = fullName, phone = rawPhone)
            .also { user ->
                if (map.containsKey(user.login))
                    throw IllegalArgumentException("A user with this phone already exists")
                else
                    map[user.login] = user
            }
    }

    /**
     * Реализуй метод loginUser(login: String, password: String) : String
     * возвращающий поле userInfo пользователя с соответствующим логином и паролем
     * (логин для пользователя phone или email,
     * пароль соответственно accessCode или password указанный при регистрации методом registerUser)
     * или возвращающий null если пользователь с указанным логином и паролем не найден (или неверный пароль)
     */
    fun loginUser(login: String, password: String): String? =
        getUserFromMap(login)?.let {
            println("user by email found in map")
            if (it.checkPassword(password)) it.userInfo
            else null
        }

    /**
     * Реализуй метод requestAccessCode(login: String) : Unit,
     * после выполнения данного метода у пользователя с соответствующим логином должен быть
     * сгенерирован новый код авторизации и помещен в свойство accessCode,
     * соответственно должен измениться и хеш пароля пользователя
     * (вызов метода loginUser должен отрабатывать корректно)
     */
    fun requestAccessCode(login: String) =
        getUserFromMap(login)?.let { user ->
            user.changePassword(user.accessCode!!, user.generateAccessCode())
        }


    /**
     * Реализуй метод importUsers(list: List): List, в качестве аргумента принимает список строк
     * где разделителем полей является ";" данные перечислены в следующем порядке
     * - Полное имя пользователя;
     * email;
     * соль:хеш пароля;
     * телефон
     * (Пример: " John Doe ;JohnDoe@unknow.com;[B@7591083d:c6adb4becdc64e92857e1e2a0fd6af84;;")
     * метод должен вернуть коллекцию список User (Пример возвращаемого userInfo:
     * firstName: John
     * lastName: Doe
     * login: johndoe@unknow.com
     * fullName: John Doe
     * initials: J D
     * email: JohnDoe@unknow.com
     * phone: null
     * meta: {src=csv}),
     * при этом meta должно содержать "src" : "csv",
     * если сзначение в csv строке пустое то соответствующее свойство в объекте User должно быть null,
     * обратите внимание что salt и hash пароля в csv разделены ":" ,
     * после импорта пользователей вызов метода loginUser должен отрабатывать корректно
     * (достаточно по логину паролю)
     */

    /**
     * Реализуй функцию расширения fun List.dropLastUntil(predicate: (T) -> Boolean): List ,
     * в качестве аргумента принимает предикат (лямбда выражение возвращающее Boolean)
     * и возвращат список в котором исключены все элементы с конца до тех пор пока не будет
     * выполнено условие предиката (элемент соответствующий условию тоже должен быть исключен
     * из результирующей коллекции) (Пример:
     * listOf(1, 2, 3).dropLastUntil{ it==2 } // [1]
     * "House Nymeros Martell of Sunspear".split(" ")
     * .dropLastUntil{ it == "of" } // [House, Nymeros, Martell])
     */

    private fun getUserFromMap(login: String): User? =
        map[login.trim()] ?: map[login.getCleanPhone()]

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}