/*
Script necessary for the repository importer to work correctly. DO NOT delete it unless you uninstalled first the Repository importer app.
Don't change the name of this script, it will allow to update it without creating another one
*/

//IMPORTANT: don't change this variable
var version = 6;

var data=LL.getEvent().getData();
if(data!=null)data=JSON.parse(data);

var intent=new Intent("android.intent.action.MAIN");
intent.setClassName("com.trianguloy.llscript.repository","com.trianguloy.llscript.repository.webViewer");
intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

if(data==null){
    //Send the id to the importer app
    intent.putExtra("id",LL.getCurrentScript().getId());
    LL.startActivity(intent);
    if(LL.getEvent().getSource()=="C_LOADED")deleteDesktop();
    return;
}


//Data received. Let's assume it is correct
if(data.update!=null){
    //apply update of this script
    var thisscript=LL.getCurrentScript();
    thisscript.setText(data.update);
    LL.runScript(thisscript.getName(),LL.getScriptTag());
    LL.setScriptTag(null);
    return;

}else if(data.version!=version){
//outdated version

    if(data.fromupdate){
        alert("WARNING! THE VERSION OF THE SCRIPT AND THE VERSION OF THE APP ARE NOT THE SAME. Contact the developer");
        return;
    }
    data.fromupdate=true;
    LL.setScriptTag(JSON.stringify(data));
    intent.putExtra("update",true);
    toast="Updating...";

}else{
//all ok, create script
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);//To open the app without loading it again
    var toast="";
    var scripts=LL.getAllScriptMatching(Script.FLAG_ALL);
    var match=null;
    for(var t=0;t<scripts.getLength();++t){
        if(scripts.getAt(t).getName()==data.name)match=scripts.getAt(t);
        //if duplicated, only the last one (oldest in most cases)
    }

    if(match==null){
    //Not found. Create
        LL.createScript(data.name,data.code,data.flags);
        toast="Script imported successfully.\nAvailable in the launcher";
    }else if(match.getText()==data.code){
    //same name and code
        toast="Script already imported, nothing changed";
    }else{
    //same name, different code
        if(confirm("There is a script with the same name but different code. Do you want to update it?")){
        //update
            match.setText(data.code);
            toast="Script updated";
        }else{
        //don't update
            toast="Not imported";
        }
    }

}

Android.makeNewToast(toast, true).show();
LL.startActivity(intent);



function deleteDesktop(){

LL.bindClass("java.io.FileReader");
LL.bindClass("java.io.BufferedReader");
LL.bindClass("java.io.FileWriter");
LL.bindClass("java.io.File");
LL.bindClass("java.lang.System");

LL.getCurrentDesktop().getProperties().edit().setEventHandler("load",EventHandler.UNSET,null).commit();

//create the needed structure
var d=LL.getDesktopByName("loadScript");
if(d==null)return;
var file=new File("data/data/net.pierrox.lightning_launcher_extreme/files/config");//this file contains desktop properties
var r=new BufferedReader(new FileReader(file));
var s="";
var dir=new File("data/data/net.pierrox.lightning_launcher_extreme/files/pages"+d.getId());
var cur=new File("data/data/net.pierrox.lightning_launcher_extreme/files/current");

//read the file
var l=r.readLine();
while(l!=null)
{
s+=l;
l=r.readLine();
}
//edit the properties
var data=JSON.parse(s);
var pos=data.screensNames.indexOf(d.getName());
if(pos>-1)data.screensNames.splice(pos,1);
var idPos=data.screensOrder.indexOf(d.getId());
if(idPos>-1)data.screensOrder.splice(idPos,1);

//write the changed stuff back to the file
var w=new FileWriter(file);
w.write(JSON.stringify(data));
w.flush();
w.close();
dir.delete();

w=new FileWriter(cur);
w.write(data.screensOrder[0]);
w.flush();
w.close();

//restart the launcher
System.runFinalization();
System.exit(0);
}