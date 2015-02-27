var data=LL.getEvent().getData();
try{
    data=JSON.parse(data);
}catch(e){
    data=null;
}

var intent=new Intent("android.intent.action.MAIN");
intent.setComponent(ComponentName.unflattenFromString("com.trianguloy.llscript.repository/com.trianguloy.llscript.repository.webViewer"));

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

Android.makeNewToast(toast, true).show();
LL.startActivity(intent);