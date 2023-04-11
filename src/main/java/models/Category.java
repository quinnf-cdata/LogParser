package models;

public class Category {
    private String name;
    private String regexPattern;
    private int group;
    private boolean groupOnly;
    private boolean allowDuplicates;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public boolean isGroupOnly() {
        return groupOnly;
    }

    public void setGroupOnly(boolean groupOnly) {
        this.groupOnly = groupOnly;
    }

    public boolean isAllowDuplicates() {
        return allowDuplicates;
    }

    public void setAllowDuplicates(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    @Override
    public String toString() {
        return "Category [name=" + name + ", regexString=" + regexPattern + ", group="+group+", groupOnly="+groupOnly+", allowDuplicates="+allowDuplicates+"]";
    }
}
