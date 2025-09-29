package com.utp.wemake.models;

public class ChecklistItem {
    public String text;
    public boolean isChecked;
    public ChecklistItem(String text, boolean isChecked) {
        this.text = text;
        this.isChecked = isChecked;
    }
}