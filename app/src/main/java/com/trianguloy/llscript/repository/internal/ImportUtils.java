package com.trianguloy.llscript.repository.internal;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.Html;

import com.trianguloy.llscript.repository.Constants;
import com.trianguloy.llscript.repository.Manifest;
import com.trianguloy.llscript.repository.R;
import com.trianguloy.llscript.repository.ScriptImporter;
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

        //initialize variables
        final ArrayList<String> names = new ArrayList<>();//names of all scripts
        final ArrayList<String> rawCodes = new ArrayList<>();//Found scripts
        String aboutScript = "";

        //Starts searching all scripts
        Elements elements = document.select(Constants.SCRIPT_SELECTORS);
        for (Element e : elements) {
            rawCodes.add(e.ownText());
            Element parent = e;
            int index = e.elementSiblingIndex();
            loop:
            while ((parent = parent.parent()) != null) {
                while (--index >= 0) {
                    Element sibling = parent.child(index);
                    if (!sibling.text().equals("")) {
                        names.add(sibling.text());
                        break loop;
                    }
                }
                index = parent.elementSiblingIndex();
            }
            if (names.size() < rawCodes.size())
                names.add(context.getString(R.string.text_nameNotFound));
        }

        //TODO search the flags

        //About script: purpose, author, link
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
                buffer.append((i == 0) ? "" : "\n *  ").append(Html.fromHtml(prov[i]).toString());
            }
            aboutScript = buffer.toString();

            //adds the beginning and end comment block, and remove extra whitespaces at the beginning and end
            aboutScript = "/* " + aboutScript.trim() + "\n */\n\n";
        }

        //switch based on the number of scripts found
        if (rawCodes.size() > 1) {
            //more than one script founds
            final String about = aboutScript;
            Dialogs.moreThanOneScriptFound(context, names.toArray(new String[names.size()]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(@NonNull DialogInterface dialog, int which) {
                    dialog.dismiss();//Necessary, this is launched when clicking an item, not when clicking a button
                    showImportScript(context, listener, names.get(which), rawCodes.get(which), about);
                }
            });
        } else if (rawCodes.size() == 1) {
            oneScriptFound(context, webView, listener, names.get(0), rawCodes.get(0), aboutScript);
        } else {
            Dialogs.noScriptFound(context);
        }
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

    private static void oneScriptFound(@NonNull Activity context, @NonNull ManagedWebView webView, @NonNull Listener listener, String name, @NonNull String rawCode, String about) {
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
        if (scriptName == null) {
            //fallback if not found in repo or repo not found
            scriptName = name;
        }

        showImportScript(context, listener, scriptName, rawCode, about);

    }

    /**
     * call to {@link Dialogs#importScript(Activity, String, String, Dialogs.OnImportListener, Dialogs.OnImportListener)}
     * to import a single script
     *
     * @param context     the context used
     * @param listener    what to do when finishing importing
     * @param scriptName  the name of the script
     * @param scriptCode  the code of the script
     * @param aboutString the header of the imported script
     */
    private static void showImportScript(@NonNull final Activity context, @NonNull final Listener listener, String scriptName, @NonNull String scriptCode, String aboutString) {

        String code = scriptCode.trim();

        if (Preferences.getDefault(context).getBoolean(context.getString(R.string.pref_aboutScript), true))
            code = aboutString + code;

        Dialogs.importScript(context, code, scriptName, new Dialogs.OnImportListener() {
            @Override
            public void onClick(String code, String name, int flags) {
                sendScriptToLauncher(context, code, name, flags);
                listener.importFinished();
            }
        }, new Dialogs.OnImportListener() {
            @Override
            public void onClick(String code, String name, int flags) {
                shareAsText(context, code, name, flags);
            }
        });

    }

    //Send & share functions
    private static void sendScriptToLauncher(@NonNull final Context context, String code, String scriptName, int flags) {
        // let's import the script
        final Intent intent = new Intent(context, ScriptImporter.class);
        intent.putExtra(Constants.EXTRA_CODE, code);
        intent.putExtra(Constants.EXTRA_NAME, scriptName);
        intent.putExtra(Constants.EXTRA_FLAGS, flags);
        PermissionActivity.checkForPermission(context, Manifest.permission.IMPORT_SCRIPTS, new PermissionActivity.PermissionCallback() {
            @Override
            public void handlePermissionResult(boolean isGranted) {
                if (isGranted) context.startService(intent);
            }
        });
    }

    private static void shareAsText(@NonNull Context context, String code, String scriptName, int flags) {
        //share the code as plain text

        StringBuilder text = new StringBuilder("");

        //flags
        text.append("//Flags: ");
        if (flags >= Constants.FLAG_CUSTOM_MENU) {
            text.append("app ");
            flags -= Constants.FLAG_CUSTOM_MENU;
        }
        if (flags >= Constants.FLAG_ITEM_MENU) {
            text.append("item ");
            flags -= Constants.FLAG_ITEM_MENU;
        }
        if (flags >= Constants.FLAG_APP_MENU) {
            text.append("custom ");
        }
        text.append("\n");

        //name
        text.append("//Name: ")
                .append(scriptName)
                .append("\n")
                .append(code);

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
