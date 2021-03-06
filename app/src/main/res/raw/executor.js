bindClass("android.util.Log");

var ScriptName = "name";
var ScriptFlags = "flags";
var ScriptCode = "code";
var extraStatus = "status";
var extraLoadedScriptId = "loadedScriptId";
var callbackId = "callbackId";
var runOnly = "runOnly";
var result = "result";

var STATUS_EVAL_FAILED = 4;
var STATUS_LAUNCHER_PROBLEM = 3;
var STATUS_UPDATE_CONFIRMATION_REQUIRED = 2;
var STATUS_OK = 1;


var data=getEvent().getData();
try{
    data=JSON.parse(data);
}catch(e){
    data=null;
}

var intent=new Intent("net.pierrox.lightning_launcher.script.IMPORT_RESPONSE");

if(data==null){
    //not a call by the app, ignore
    return;
}

//Data received. Let's assume it is correct
intent.putExtra(callbackId, data.callbackId);

if(data.runOnly){
    try{
        eval("function executor_toRun(){\n"+ data.code+"\n}");
        intent.putExtra(result, executor_toRun());
        intent.putExtra(extraStatus, STATUS_OK);
    }catch(e){
        intent.putExtra(extraStatus, STATUS_EVAL_FAILED);
        Log.w("[REPOSITORY IMPORTER]", "Failed to evaluate Script", e.javaException);
    }
}
else{
    //create script
    var toast="";
    var scripts=getAllScriptMatching(Script.FLAG_ALL);
    var match=null;
    for(var t=0;t<scripts.length;++t){
        var s = scripts[t];
        if(s.getName()==data.name && s.getPath() == data.path)match=s;
        //if duplicated, only the last one (oldest in most cases)
    }

    if(match==null){

    //Not found. Create
        match = createScript(data.path,data.name,data.code,data.flags);
        intent.putExtra(extraStatus,STATUS_OK);
        toast="Script imported successfully.\nAvailable in the launcher";

    }else if(match.getText()==data.code){

    //same name and code. Update flags
        intent.putExtra(extraStatus,STATUS_OK);
        toast="Script already imported. Flags updated.";
        //2,4,8 <-> app_menu,item_menu,custom_menu
        match.setFlag(2,((data.flags>>1)&1)==1);
        match.setFlag(4,((data.flags>>2)&1)==1);
        if(((data.flags>>3)&1)==1)match.setFlag(8,((data.flags>>3)&1)==1);//only set the flag if necessary, don't remove it

    }else{

    //same name, different code
        if(data.forceUpdate){
            //update
            match.setText(data.code);
            //2,4,8 <-> app_menu,item_menu,custom_menu
            match.setFlag(2,((data.flags>>1)&1)==1);
            match.setFlag(4,((data.flags>>2)&1)==1);
            if(((data.flags>>3)&1)==1)match.setFlag(8,((data.flags>>3)&1)==1);//only set the flag if necessary, don't remove it

            intent.putExtra(extraStatus,STATUS_OK);
            toast="Script updated";
        }else{
        //ask caller what to do
        intent.putExtra(extraStatus,STATUS_UPDATE_CONFIRMATION_REQUIRED);
        }
    }
    intent.putExtra(extraLoadedScriptId,match.getId());

    if(toast!="")Toast.makeText(getActiveScreen().getContext(), toast, Toast.LENGTH_SHORT).show();
}
getActiveScreen().getContext().sendBroadcast(intent);