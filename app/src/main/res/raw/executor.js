var ScriptName = "name";
var ScriptFlags = "flags";
var ScriptCode = "code";
var extraStatus = "status";
var extraLoadedScriptId = "loadedScriptId";

var STATUS_LAUNCHER_PROBLEM = 3;
var STATUS_UPDATE_CONFIRMATION_REQUIRED = 2;
var STATUS_OK = 1;


var data=LL.getEvent().getData();
try{
    data=JSON.parse(data);
}catch(e){
    data=null;
}

var intent=new Intent("android.intent.action.View");
intent.setComponent(ComponentName.unflattenFromString("com.trianguloy.llscript.repository/com.trianguloy.llscript.repository.IntentHandle"));

if(data==null){

    //From previous versions, delete the saved script
    var thisscript=LL.getCurrentScript();
    if(thisscript.getId() != -3){
    LL.deleteScript(thisscript);
    Android.makeNewToast("Removed old manager",true).show();
    }

    //Open the app
    LL.startActivity(intent);
    return;
}

//Data received. Let's assume it is correct

//create script
var toast="";
var scripts=LL.getAllScriptMatching(Script.FLAG_ALL);
var match=null;
for(var t=0;t<scripts.getLength();++t){
    if(scripts.getAt(t).getName()==data.name)match=scripts.getAt(t);
    //if duplicated, only the last one (oldest in most cases)
}


intent.setComponent(ComponentName.unflattenFromString(data.returnTo));

if(match==null){

//Not found. Create
    match = LL.createScript(data.name,data.code,data.flags);
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
    //send parameters back for convenience
    intent.putExtra(ScriptName,data.name);
    intent.putExtra(ScriptCode,data.code);
    intent.putExtra(ScriptFlags,data.flags);
    }
}
intent.putExtra(extraLoadedScriptId,match.getId());

if(toast!="")Android.makeNewToast(toast, true).show();
LL.startActivity(intent);