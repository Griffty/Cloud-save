package org.griffty;

public class UserManager {
    private static UserManager instance;
    public static UserManager getInstance() {
        if (instance == null){
            instance = new UserManager();
        }
        return instance;
    }
    UserManager(){
        updateSystemUserList();
    }

    private void updateSystemUserList() {
    //todo: upload Users From Save File
    }
}
