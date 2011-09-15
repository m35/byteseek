/*
 * Copyright Matt Palmer 2011, All rights reserved.
 *
 */

package net.domesdaybook.reader;

import java.util.HashMap;

/**
 *
 * @author matt
 */
public final class WindowAllCache extends HashMap<Long, Window> implements WindowCache {

    
    @Override
    public Window getWindow(final long position) {
        return get(position);
    }

    
    @Override
    public void addWindow(final Window window) {
        final long position = window.getWindowPosition();
        put(position, window);
    }

    @Override
    public WindowCache newInstance() {
        return new WindowAllCache();
    }
    
}
