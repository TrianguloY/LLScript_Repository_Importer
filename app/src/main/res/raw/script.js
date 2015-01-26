/*
Script necessary for the repository importer to work correctly. DO NOT delete it unless you uninstalled first the Repository importer app.
Don't change the name of this script, it will allow to update it without creating another one
*/

//IMPORTANT: don't change this variables
var version = 3;

if(LL.getEvent().getSource()=="C_LOADED")LL.getCurrentDesktop().getProperties().edit().setEventHandler("load",EventHandler.UNSET,null).commit();

var data=LL.getEvent().getData();
if(data!=null)data=JSON.parse(data);

var intent=new Intent("android.intent.action.MAIN");
intent.setClassName("com.trianguloy.llscript.repository","com.trianguloy.llscript.repository.webViewer");


if(data==null){
    //Send the id to the importer app
    intent.putExtra("id",LL.getCurrentScript().getId());
    LL.startActivity(intent);
    deleteDesktop();
}
else{
    //Data received. Let's assume it is correct
    if(data.version!=version){
        //outdated version
        if(confirm("The script manager is outdated. Do you wish to reimport it via template (The launcher will restart in this process)?\nAlternatively you can manually reimport it from the scripts menu")){
            var applyIntent = new Intent("android.intent.action.MAIN");
            applyIntent.setClassName("com.trianguloy.llscript.repository","com.app.lukas.template.ApplyTemplate");
            LL.startActivity(applyIntent);
        }
        return;
    }

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


    Android.makeNewToast(toast, true).show();
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
LL.startActivity(intent);
}


function deleteDesktop(){
 LL.bindClass("java.io.FileReader");
LL.bindClass("java.io.BufferedReader");
LL.bindClass("java.io.FileWriter");
LL.bindClass("java.io.File");
LL.bindClass("java.lang.System");

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