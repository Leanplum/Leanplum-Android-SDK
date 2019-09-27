package com.leanplum.callbacks;

import android.content.Context;

/**
 * Callback that gets run when an embedded HREF url is selected
 *
 * @author Santiago Castaneda Munoz - Tilting Point
 */
public abstract class EmbeddedHTMLUrlCallback {

    /**
     * Specify Handling of Embedded Href Urls in HTML In App Messages
     * <code>onEmbeddedUrl</code> is called when attempting to display a WebView
     * in the HTML InAppMessage. Utilize this method to specify how to handle specific
     * links / deeplinks in Href Urls.
     * This method should return if the link was handled accordingly or not
     */
    public abstract boolean onEmbeddedUrl(Context context, String url);
}
