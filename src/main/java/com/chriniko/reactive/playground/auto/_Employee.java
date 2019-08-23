package com.chriniko.reactive.playground.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

/**
 * Class _Employee was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Employee extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "id";

    public static final Property<String> FIRSTNAME = new Property<String>("firstname");
    public static final Property<String> ID = new Property<String>("id");
    public static final Property<String> INITIALS = new Property<String>("initials");
    public static final Property<String> SURNAME = new Property<String>("surname");

    public void setFirstname(String firstname) {
        writeProperty("firstname", firstname);
    }
    public String getFirstname() {
        return (String)readProperty("firstname");
    }

    public void setId(String id) {
        writeProperty("id", id);
    }
    public String getId() {
        return (String)readProperty("id");
    }

    public void setInitials(String initials) {
        writeProperty("initials", initials);
    }
    public String getInitials() {
        return (String)readProperty("initials");
    }

    public void setSurname(String surname) {
        writeProperty("surname", surname);
    }
    public String getSurname() {
        return (String)readProperty("surname");
    }

    protected abstract void onPostAdd();

    protected abstract void onPreRemove();

    protected abstract void onPreUpdate();

    protected abstract void onPostPersist();

    protected abstract void onPostRemove();

    protected abstract void onPostUpdate();

    protected abstract void onPostLoad();

    protected abstract void onPrePersist();

}