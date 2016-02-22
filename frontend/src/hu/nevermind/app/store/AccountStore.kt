package hu.nevermind.app.store

import com.github.andrewoma.flux.Store
import hu.nevermind.app.*

enum class Role {
    ROLE_ADMIN, ROLE_USER
}

data class Account(var username: String = "",
                   var disabled: Boolean = false,
                   var role: Role,
                   var plainPassword: String) {

}

object AccountStore : Store() {

    private var accounts: MutableList<Account> = arrayListOf()
    var editingAccount: EditingAccount? = null
        private set


    init {
        if (LoggedInUserStore.loggedInUser.role != Role.ROLE_ADMIN) {
            register(globalDispatcher, Actions.setLoggedInUser) { loggedInUser ->
                if (loggedInUser == null) {
                    accounts = arrayListOf()
                } else {
                    communicator.getEntitiesFromServer(RestUrl.getAccountsFromServer) { returnedArray ->
                        val newAccounts = returnedArray.map { json ->
                            Account(json.username, json.disabled, json.role, "")
                        }.toTypedArray()
                        accounts = newAccounts.toArrayList()
                    }
                }
                emitChange()
            }
            register(globalDispatcher, Actions.setEditingAccount) { editingAccountPayload ->
                if (editingAccountPayload != editingAccount) {
                    editingAccount = editingAccountPayload
                    emitChange()
                }
            }
            register(globalDispatcher, Actions.modifyAccount) { modifiedAccount ->
                val sendingEntity = object {
                    val username = modifiedAccount.username
                    val role = modifiedAccount.role.name
                    val passwordHash = modifiedAccount.plainPassword
                    val disabled = modifiedAccount.disabled
                }
                communicator.saveEntity(RestUrl.saveAccount, sendingEntity) {
                    val index = accounts.indexOfFirst { it.username == modifiedAccount.username }
                    if (index == -1) {
                        accounts.add(modifiedAccount)
                    } else {
                        accounts[index] = modifiedAccount
                    }
                    emitChange()
                }
            }
        }
    }

    fun accounts(): List<Account> = accounts

    fun account(username: String): Account? {
        return accounts().firstOrNull { it.username == username.orEmpty() }
    }
}
