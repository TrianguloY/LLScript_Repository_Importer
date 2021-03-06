package com.trianguloy.llscript.repository.internal;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.Html;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.Manifest;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.ScriptImporter;
import com.trianguloy.llscript.repository.aidl.Failure;
import com.trianguloy.llscript.repository.aidl.IImportCallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;
import com.trianguloy.llscript.repository.aidl.Script;
import com.trianguloy.llscript.repository.settings.Preferences;
import com.trianguloy.llscript.repository.web.ManagedWebView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lukas on 04.09.2015.
 * Manages importing of scripts
 */
public final class ImportUtils {
    private ImportUtils() {
    }

    public static void startImport(@NonNull final Activity context, @NonNull final ManagedWebView webView, @NonNull final Listener listener) {
        Document document = Jsoup.parse(webView.getCurrentDocument().outerHtml(), context.getString(R.string.link_server));
        final List<Script> scripts = findScripts(context, document);
        //TODO search the flags
        String aboutScript = generateAboutComment(document);
        //switch based on the number of scripts found
        if (scripts.size() > 1) {
            //more than one script founds
            final String about = aboutScript;
            final String[] names = new String[scripts.size()];
            for (int i = 0; i < scripts.size(); i++) {
                names[i] = scripts.get(i).getName();
            }
            Dialogs.moreThanOneScriptFound(context, names, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(@NonNull DialogInterface dialog, int which) {
                    dialog.dismiss();//Necessary, this is launched when clicking an item, not when clicking a button
                    showImportScript(context, listener, scripts.get(which), about);
                }
            });
        } else if (scripts.size() == 1) {
            oneScriptFound(context, webView, listener, scripts.get(0), aboutScript);
        } else {
            Dialogs.noScriptFound(context);
        }
    }

    @NonNull
    private static List<Script> findScripts(@NonNull Context context, @NonNull Document document) {//Starts searching all scripts
        Elements elements = document.select(Constants.SCRIPT_SELECTORS);
        ArrayList<Script> list = new ArrayList<>();
        for (Element e : elements) {
            String code = e.ownText();
            String name = null;
            Element parent = e;
            int index = e.elementSiblingIndex();
            loop:
            while ((parent = parent.parent()) != null) {
                while (--index >= 0) {
                    Element sibling = parent.child(index);
                    if (!sibling.text().equals("")) {
                        name = sibling.text();
                        break loop;
                    }
                }
                index = parent.elementSiblingIndex();
            }
            if (name == null) {
                name = context.getString(R.string.text_nameNotFound);
            }
            list.add(new Script(code, name, 0));
        }
        return list;
    }

    private static String generateAboutComment(@NonNull Document document) {
        //About script: purpose, author, link
        String aboutScript = "";
        Elements aboutElements = document.select("#about_the_script");
        if (aboutElements.size() > 0) {

            Element about = aboutElements.first();
            //remove html tags
            aboutScript = about.text();
            Element parent = about.parent();
            int index = about.elementSiblingIndex();
            Element e = parent.child(index + 1);
            aboutScript += getTextWithLineBreaks(e);

            String[] prov = aboutScript.split("\n+");//separate the text removing duplicated line breaks

            //join the text adding an asterisk at the beginning of each line and converting the html string into normal code
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < prov.length; ++i) {
                //noinspection deprecation
                buffer.append((i == 0) ? "" : "\n *  ").append(Html.fromHtml(prov[i]).toString());
            }
            aboutScript = buffer.toString();

            //adds the beginning and end comment block, and remove extra whitespaces at the beginning and end
            aboutScript = "/* " + aboutScript.trim() + "\n */\n\n";
        }
        return aboutScript;
    }

    private static String getTextWithLineBreaks(@NonNull Element e) {
        StringBuilder builder = new StringBuilder();
        List<Node> children = e.childNodes();
        if (children.size() > 0) {
            for (Node child : children) {
                if (child instanceof Element) {
                    Element c = (Element) child;
                    if (Tag.valueOf("li") == c.tag()) builder.append("\n");
                    builder.append(getTextWithLineBreaks(c));
                } else if (child instanceof TextNode) {
                    builder.append(((TextNode) child).text());
                }
            }
        } else {
            builder.append(e.text());
        }
        return builder.toString();
    }

    private static void oneScriptFound(@NonNull Activity context, @NonNull ManagedWebView webView, @NonNull Listener listener, @NonNull Script script, String about) {
        //only one script, load directly

        //get the name from the repository
        String url = webView.getUrl();
        assert url != null;
        url = url.substring(url.indexOf('/', "http://www".length()));
        Document repoDocument = webView.getRepoDocument();
        String scriptName = null;
        if (repoDocument != null) {
            Elements elements = repoDocument.select("a[href*=" + url + "]");
            if (elements.size() > 0) {
                scriptName = elements.first().ownText();
            }
        }
        if (scriptName != null) {
            script.setName(scriptName);
        }

        showImportScript(context, listener, script, about);

    }

    /**
     * call to {@link Dialogs#importScript(Activity, Script, Dialogs.OnImportListener, Dialogs.OnImportListener)}
     * to import a single script
     *
     * @param context     the context used
     * @param listener    what to do when finishing importing
     * @param script      the script
     * @param aboutString the header of the imported script
     */
    private static void showImportScript(@NonNull final Activity context, @NonNull final Listener listener, @NonNull final Script script, String aboutString) {

        String code = script.getCode().trim();

        if (Preferences.getDefault(context).getBoolean(R.string.pref_aboutScript, true)) {
            code = aboutString + code;
        }
        script.setCode(code);

        Dialogs.importScript(context, script, new Dialogs.OnImportListener() {
            @Override
            public void onClick(Script script) {
                sendScriptToLauncher(context, script);
                listener.importFinished();
            }
        }, new Dialogs.OnImportListener() {
            @Override
            public void onClick(Script script) {
                shareAsText(context, script);
            }
        });

    }

    //Send & share functions

    /**
     * sends a script to LL for import
     *
     * @param context a context
     * @param script  the script
     */
    private static void sendScriptToLauncher(@NonNull final Context context, final Script script) {
        PermissionActivity.checkForPermission(context, Manifest.permission.IMPORT_SCRIPTS, new PermissionActivity.PermissionCallback() {
            @Override
            public void handlePermissionResult(boolean isGranted) {
                if (isGranted) {
                    // let's import the script
                    context.bindService(new Intent(context, ScriptImporter.class), new ServiceConnection() {
                        final ServiceConnection connection = this;

                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            final ILightningService lightningService = ILightningService.Stub.asInterface(service);
                            try {
                                lightningService.importScript(script, false, new IImportCallback.Stub() {
                                    @Override
                                    public void onFinish(int scriptId) throws RemoteException {
                                        context.unbindService(connection);
                                    }

                                    @Override
                                    public void onFailure(Failure failure) throws RemoteException {
                                        if (failure == Failure.SCRIPT_ALREADY_EXISTS) {
                                            Dialogs.confirmUpdate(context, script, lightningService, new Runnable() {
                                                @Override
                                                public void run() {
                                                    context.unbindService(connection);
                                                }
                                            });
                                        } else {
                                            context.unbindService(connection);
                                        }
                                    }
                                });
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {

                        }
                    }, Context.BIND_AUTO_CREATE);
                }
            }
        });
    }

    private static void shareAsText(@NonNull Context context, Script script) {
        //share the code as plain text

        StringBuilder text = new StringBuilder("");

        //flags
        text.append("//Flags: ");
        int flags = script.getFlags();
        if ((flags & Script.FLAG_CUSTOM_MENU) != 0) {
            text.append("app ");
        }
        if ((flags & Script.FLAG_ITEM_MENU) != 0) {
            text.append("item ");
        }
        if ((flags & Script.FLAG_APP_MENU) != 0) {
            text.append("custom ");
        }
        text.append("\n");

        //name
        text.append("//Name: ")
                .append(script.getName())
                .append("\n")
                .append(script.getCode());

        text.append("\n");

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text.toString());
        context.startActivity(Intent.createChooser(share, "Send to..."));
    }

    public interface Listener {
        void importFinished();
    }
}
