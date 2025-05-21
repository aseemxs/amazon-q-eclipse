// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.views;

import java.util.concurrent.Future;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import software.aws.toolkits.eclipse.amazonq.chat.ChatCommunicationManager;
import software.aws.toolkits.eclipse.amazonq.chat.ChatStateManager;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.providers.assets.ChatWebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.providers.assets.WebViewAssetProvider;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;

public class AmazonQChatWebview extends AmazonQView implements ChatUiRequestListener {

    public static final String ID = "software.aws.toolkits.eclipse.amazonq.views.AmazonQChatWebview";

    private final ChatStateManager chatStateManager;
    private final ChatCommunicationManager chatCommunicationManager;
    private Browser browser;
    private volatile boolean canDisposeState = false;
    private WebViewAssetProvider webViewAssetProvider;
    private Future<?> refreshFuture;

    public AmazonQChatWebview() {
        super();
        chatStateManager = ChatStateManager.getInstance();
        chatCommunicationManager = ChatCommunicationManager.getInstance();
        webViewAssetProvider = new ChatWebViewAssetProvider();
        webViewAssetProvider.initialize();
    }

    @Override
    public final Composite setupView(final Composite parent) {
        setupParentBackground(parent);
        browser = chatStateManager.getBrowser(parent);
        // attempt to use existing browser with chat history if present, else create a
        // new one
        if (browser == null || browser.isDisposed()) {
            canDisposeState = false;
            var result = setupBrowser(parent);
            // if setup of amazon q view fails due to missing webview dependency, switch to
            // that view and don't setup rest of the content
            if (!result) {
                return parent;
            }

            browser = getAndUpdateStateManager();

            browser.setVisible(false);
            browser.addProgressListener(new ProgressAdapter() {
                @Override
                public void completed(final ProgressEvent event) {
                    Display.getDefault().asyncExec(() -> {
                        if (!browser.isDisposed()) {
                            browser.setVisible(true);
                            chatCommunicationManager.activate();
                        }
                    });
                }
            });

            webViewAssetProvider.injectAssets(browser);
        } else {
            updateBrowser(browser);
        }

        super.setupView(parent);
        parent.addDisposeListener(e -> chatStateManager.preserveBrowser());
        chatCommunicationManager.setChatUiRequestListener(this);
        addFocusListener(parent, browser);
        setupAmazonQCommonActions();
        checkAndRestartRefreshThread();

        return parent;
    }

    private Browser getAndUpdateStateManager() {
        var browser = getBrowser();
        chatStateManager.updateBrowser(browser);
        return browser;
    }

    @Override
    public final void onSendToChatUi(final String message) {
        checkAndRestartRefreshThread();
        String script = "window.postMessage(" + message + ");";
        Display.getDefault().asyncExec(() -> {
            browser.execute(script);
        });
    }

    public final void disposeBrowserState() {
        canDisposeState = true;
    }

    private void setupPeriodicRefresh(final Browser browser) {
        if (refreshFuture != null && !refreshFuture.isDone()) {
            return;
        }

        Runnable refreshTask = new Runnable() {
            @Override
            public void run() {
                if (!Display.getDefault().isDisposed()) {
                    Display.getDefault().asyncExec(() -> {
                        if (browser != null && !browser.isDisposed()) {
                            browser.execute("""
                                document.querySelectorAll('[class*="mynah-ui-icon-"]').forEach(icon => {
                                    const computed = window.getComputedStyle(icon);
                                    const webkitMask = computed.getPropertyValue('-webkit-mask-image');
                                    const standardMask = computed.getPropertyValue('mask-image');
                                    icon.style.webkitMaskImage = '';
                                    icon.style.maskImage = '';
                                    icon.offsetHeight;
                                    icon.style.webkitMaskImage = webkitMask;
                                    icon.style.maskImage = standardMask;
                                });
                            """);
                        }
                    });
                }
                refreshFuture = ThreadingUtils.scheduleAsyncTaskWithDelay(this, 15000);
            }
        };
        refreshFuture = ThreadingUtils.scheduleAsyncTaskWithDelay(refreshTask, 15000);
    }

    private void checkAndRestartRefreshThread() {
        if (refreshFuture == null || refreshFuture.isDone() || refreshFuture.isCancelled()) {
            Activator.getLogger().warn("Periodic refresh task not running, restarting...");
            setupPeriodicRefresh(this.browser);
        }
    }

    @Override
    public final void dispose() {
        chatCommunicationManager.removeListener(this);
        if (canDisposeState) {
            ChatStateManager.getInstance().dispose();
        }
        if (refreshFuture != null) {
            refreshFuture.cancel(true);
        }
        super.dispose();
    }

}
