package com.example.asynctask;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

@SuppressLint("NewApi")
public class MyAsyncTask extends AsyncTask<String, Void, String> {

	private Context context;
	private PrintWriter printWriter;

	public MyAsyncTask(Context context) {
        this.context = context;
    }
	
	
	@Override
	
	protected String doInBackground(String... params) {
		String type;
		if (params.length>0){
			type=params[0];
			Intent i=new Intent();
			switch (type) {
			//launch broadcast 
			  case "broadcast":
				   if (params.length==4){
					  	i.setComponent(new ComponentName(params[1], params[2]));
					  	final String command = params[3];
					  	final Intent myint=i;
		        		final String t=type;
					  	Thread thread = new Thread() {
					  	    @Override
					  	    public void run() {
					  	    	try {
					  	    		PrintWriter all_intents= new PrintWriter(new FileOutputStream(Environment.getExternalStorageDirectory()+"/test/all_"+t +".sh", true /* append = true */));
									all_intents.write("adb shell" + command);
									all_intents.write(System.lineSeparator());
									all_intents.close();
					  	    		Log.d("BIFUZ_BROADCAST ",command + "\n " );
					  	    		System.out.println("BIFUZ_BROADCAST "+ command + "\n " );
									context.sendBroadcast(myint);
								 }
								catch(Exception e) {
								    Log.e("Fuzz exception:", e.toString());
								}
								catch(Error e2) {
								    Log.e("Fuzz error:", e2.toString());
								}
					  	    }
					  	};
					  	thread.start();
						try {
							thread.join();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return type+ "_" + params[2]  ;
				   }
				  return type+"_Bad Args";
			//launch intent
			  case "intent":
				  if (params.length==11){
					  	i.setComponent(new ComponentName(params[1], params[2]));
					  	i.setAction(params[3]);
							i.addCategory(params[4]);
							int f=Integer.parseInt(params[5].substring(3),16);
							i.addFlags(f);
							i.setData(Uri.parse(params[6]));
							String extra_type = params[7];
							if (extra_type.equals("boolean")){
								i.putExtra(params[8], Boolean.parseBoolean(params[9]));
							}
							else {
								i.putExtra(params[8], Integer.parseInt(params[9].split("\n")[0]));
							}
							final String command = params[10];
							final Intent myint=i;
							final String t=type;
							Thread thread = new Thread() {
						  	    @Override
						  	    public void run() {
						  	    	try {
						  	    		PrintWriter all_intents= new PrintWriter(new FileOutputStream(Environment.getExternalStorageDirectory()+"/test/all_"+t +".sh", true /* append = true */));
						  	    		all_intents.append("adb shell" + command);
										all_intents.append(System.lineSeparator());
										all_intents.close();
						  	    		Log.d("BIFUZ_INTENT ",command + "\n " );
						  	    		System.out.println("BIFUZ_INTENT "+ command + "\n " );
						  	    		context.startActivity(myint);
									 }
									catch(Exception e) {
									    Log.e("Fuzz exception:", e.toString());									    
									}
									catch(Error e2) {
									    Log.e("Fuzz error:", e2.toString());									   
									}
						  	    }
							};
							thread.start();
							try {
								thread.join();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						return  type+ "_" + params[2] ;
				   }
				  return type+"_Bad Args";
			  default:
				  return type+"_Bad Args";
			}
		
				
			}
			else return "Bad Args";
		
		
	}
	
	@Override
    protected void onPostExecute(String result) {
		
		//create file for logcat
		String filename;
		if (result.contains("Error"))
			filename="logcat_testError" +".txt";
		else filename="logcat_"+result +".txt";
		String[] res = result.split("_");
		String pack_name = res[1];
		String type = res[0];
		File outputFile = new File(Environment.getExternalStorageDirectory(),filename);
		String filePath = outputFile.getAbsolutePath();

		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = am.getRunningAppProcesses();
		for (int a = 0; a < runningAppProcessInfo.size(); a++) {
			System.out.println(runningAppProcessInfo.get(a).processName );
			
			
		}
		
	//GET LOGCAT	
		
		try {
		    outputFile.createNewFile();
		    String[] CMDLINE_GRANTPERMS = { "su", "-c", null };
		    CMDLINE_GRANTPERMS[2]="logcat -d -f "+outputFile.getAbsolutePath();
		    String cmd = "logcat -d -f "+outputFile.getAbsolutePath();
		    Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		
		//PARSE LOGCAT and CREATE SEED FILE IF error is found		
		try {
		    String[] CMDLINE_GRANTPERMS = { "su", "-c", null };
		    CMDLINE_GRANTPERMS[2]="logcat -c ";
		    Runtime.getRuntime().exec(CMDLINE_GRANTPERMS);
		    BufferedReader log_file=new BufferedReader(new FileReader(outputFile.getAbsolutePath()));
		    try {
		        String line = log_file.readLine();

		        while (line != null) {
		        	if (line.contains("E/") && line.contains(pack_name) && !line.contains("at")){
		        		String p = "com"+ line.split("com")[1].split(":")[0];
		        		String e = line.split(":")[1];
		        		String seedfile = p+"."+e+".sh";
		        		File seed_file = new File(Environment.getExternalStorageDirectory(),"/test/"+seedfile);
		        		seed_file.createNewFile();
		        		PrintWriter seed = new PrintWriter(seed_file);
		        		
		        		BufferedReader all_intents=new BufferedReader(new FileReader(Environment.getExternalStorageDirectory()+"/test/all_"+type +".sh"));
		        		try {
		    		        String intent = all_intents.readLine();
		    		        while (intent != null) {
		    		        	seed.append(intent);
		    		        	intent = all_intents.readLine();
		    		        	if (intent!="\n") seed.append(System.lineSeparator());
		    		        }
		        		}
		        		 finally {
		        			 all_intents.close();
		        			 seed.close();
		        		}
		        	}
		        	line = log_file.readLine();
		        }
		    } finally {
		    	log_file.close();
		    }
		    
		    
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		

		
    }

}
