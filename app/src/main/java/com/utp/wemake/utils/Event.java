package com.utp.wemake.utils;

/**
 * Una clase envoltorio (wrapper) para datos que se exponen a través de LiveData
 * y que representan un evento que debe ser consumido una sola vez.
 *
 * @param <T> El tipo de contenido que envolverá el evento. Puede ser un String, un Enum, etc.
 */
public class Event<T> {

    private final T content;
    private boolean hasBeenHandled = false;

    /**
     * Constructor que recibe el contenido del evento.
     */
    public Event(T content) {
        this.content = content;
    }

    /**
     * Devuelve el contenido del evento y evita que se vuelva a usar.
     * Este es el método que debes llamar desde tu observador en el Fragment o Activity.
     * Si el evento ya fue manejado, devuelve null.
     */
    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null;
        } else {
            hasBeenHandled = true;
            return content;
        }
    }

    /**
     * Devuelve el contenido incluso si ya ha sido manejado.
     * Es útil principalmente para depuración o vistas previas, no para la lógica principal.
     */
    public T peekContent() {
        return content;
    }
}