package com.hacklympics.utility.dialog;

public class ConfirmDialog extends Dialog {
    
    public ConfirmDialog(String title, String body) {
        super(title);
        
        content.setBody(new WrappingText(body, container.getWidth()));
    }
    
}