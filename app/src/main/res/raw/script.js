/*
Script necessary for the repository importer to work correctly. DO NOT delete it unless you uninstalled first the Repository importer app.
Don't change the name of this script, it will allow to update it without creating another one
*/

//IMPORTANT: don't change this variable
var version = 16;

var data=LL.getEvent().getData();
try{
    data=JSON.parse(data);
}catch(e){
    data=null;
}

var intent=new Intent("android.intent.action.MAIN");
intent.setComponent(ComponentName.unflattenFromString("com.trianguloy.llscript.repository/com.trianguloy.llscript.repository.webViewer"));

if(data==null){
    //Send the id to the importer app
    intent.putExtra("id",LL.getCurrentScript().getId());
    LL.startActivity(intent);
    return;
}

//Data received. Let's assume it is correct
if(data.version!=version){
//outdated version

    if(data.fromupdate){
        alert("WARNING! THE VERSION OF THE SCRIPT AND THE VERSION OF THE APP ARE NOT THE SAME. Contact the developer");
        return;
    }
    data.fromupdate=true;
    LL.setScriptTag(JSON.stringify(data));
    Android.makeNewToast("Updating...",true).show();

    LL.bindClass("android.content.ServiceConnection");
    LL.bindClass("android.os.Messenger");
    LL.bindClass("android.os.Message");
    LL.bindClass("android.os.Handler");
    LL.bindClass("android.content.Context");

    var conn = new ServiceConnection(){
        onServiceConnected:function(className,service){
            var serviceMessenger = new Messenger(service);
            var messenger = new Messenger(new Handler(new Handler.Callback(){
                handleMessage:function(msg){
                    var thisscript=LL.getCurrentScript();
                    thisscript.setText(msg.getData().getCharSequence("code",thisscript.getText()).toString());
                    LL.runScript(thisscript.getName(),LL.getScriptTag());
                    LL.setScriptTag(null);
                    LL.getContext().unbindService(conn);
                    return true;
                }
            }));
            var message = Message.obtain();
            message.replyTo = messenger;
            serviceMessenger.send(message);
            return;
        },
        onServiceDisconnected:function(className){
            alert("WARNING! THE VERSION OF THE SCRIPT AND THE VERSION OF THE APP ARE NOT THE SAME. Contact the developer");
            return;
        }
    };
    var serviceIntent = new Intent();
    serviceIntent.setComponent(ComponentName.unflattenFromString("com.trianguloy.llscript.repository/com.app.lukas.llscript.ScriptUpdateService"));
    LL.getContext().bindService(serviceIntent,conn,Context.BIND_AUTO_CREATE);
    return;

}else{
//all ok, create script
    var toast="";
    var scripts=LL.getAllScriptMatching(Script.FLAG_ALL);
    var match=null;
    for(var t=0;t<scripts.getLength();++t){
        if(scripts.getAt(t).getName()==data.name)match=scripts.getAt(t);
        //if duplicated, only the last one (oldest in most cases)
    }

    if(match==null){
    //Not found. Create
        match = LL.createScript(data.name,data.code,data.flags);
        toast="Script imported successfully.\nAvailable in the launcher";
    }else if(match.getText()==data.code){
    //same name and code
        toast="Script already imported, nothing changed";
    }else{
    //same name, different code
        if(data.forceUpdate || confirm("There is a script with the same name but different code. Do you want to update it?")){
        //update
            match.setText(data.code);
            toast="Script updated";
        }else{
        //don't update
            toast="Not imported";
        }
    }
    intent.setComponent(ComponentName.unflattenFromString(data.returnTo));
    intent.putExtra("loadedScriptId",match.getId());
}

Android.makeNewToast(toast, true).show();
LL.startActivity(intent);