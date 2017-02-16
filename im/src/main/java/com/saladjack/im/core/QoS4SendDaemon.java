package com.saladjack.im.core;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import com.saladjack.im.IMCore;
import com.saladjack.im.protocal.Protocal;
import com.saladjack.im.utils.BetterAsyncTask;

import android.content.Context;
import android.os.Handler;
import android.util.Log;


/**
 * Created by SaladJack on 2017/1/15.
 */
public class QoS4SendDaemon
{
	private final static String TAG = QoS4SendDaemon.class.getSimpleName();

	private ConcurrentHashMap<String, Protocal> sentMessages = new ConcurrentHashMap<String, Protocal>();

	private ConcurrentHashMap<String, Long> sendMessagesTimestamp = new ConcurrentHashMap<String, Long>();


	public final static int CHECH_INTERVAL = 5000;


	public final static int MESSAGES_JUST$NOW_TIME = 3 * 1000;


	public final static int QOS_TRY_COUNT = 3;

	private Handler handler = null;
	private Runnable runnable = null;

	private boolean running = false;

	private boolean executing = false;

	private Context context = null;

	private static QoS4SendDaemon instance = null;


	public static QoS4SendDaemon getInstance(Context context) {
		if(instance == null)
			instance = new QoS4SendDaemon(context);

		return instance;
	}

	private QoS4SendDaemon(Context context) {
		this.context = context;
		init();
	}




	protected void notifyMessageLost(ArrayList<Protocal> lostMessages) {
		if(IMCore.getInstance().getMessageQoSEvent() != null)
			IMCore.getInstance().getMessageQoSEvent().messagesLost(lostMessages);
	}


	public void startup(boolean immediately) {
		//
		stop();

		//
		handler.postDelayed(runnable, immediately ? 0 : CHECH_INTERVAL);
		//
		running = true;
	}


	public void stop() {
		//
		handler.removeCallbacks(runnable);
		//
		running = false;
	}


	public boolean isRunning()
	{
		return running;
	}

	boolean exist(String fingerPrint)
	{
		return sentMessages.get(fingerPrint) != null;
	}


	public void put(Protocal p) {
		if(p == null) {
			Log.w(TAG, "Invalid arg p==null.");
			return;
		}
		if(p.getFp() == null) {
			Log.w(TAG, "Invalid arg p.getFp() == null.");
			return;
		}

		if(!p.isQoS()) {
			Log.w(TAG, "This protocal is not QoS pkg, ignore it!");
			return;
		}
		if(sentMessages.get(p.getFp()) != null)
			Log.w(TAG, "【QoS】指纹为"+p.getFp()+"的消息已经放入了发送质量保证队列，该消息为何会重复？（生成的指纹码重复？还是重复put？）");

		sentMessages.put(p.getFp(), p);
		sendMessagesTimestamp.put(p.getFp(), System.currentTimeMillis());
	}

	public void remove(final String fingerPrint) {
		// remove it
		new BetterAsyncTask(){
			@Override
			protected Object doInBackground(Object... params)
			{
				sendMessagesTimestamp.remove(fingerPrint);
				return sentMessages.remove(fingerPrint);
			}
			protected void onPostExecute(Object result)
			{
				Log.w(TAG, "【QoS】指纹为"+fingerPrint+"的消息已成功从发送质量保证队列中移除(可能是收到接收方的应答也可能是达到了重传的次数上限)，重试次数="
						+(result != null?((Protocal)result).getRetryCount():"none呵呵."));
			}
		}.executeParallel();
	}


	public int size()
	{
		return sentMessages.size();
	}

	private void init() {
		handler = new Handler();
		runnable = new Runnable()
		{
			@Override
			public void run()
			{
				if(!executing) {
					new BetterAsyncTask<Object, Integer, ArrayList<Protocal>>() {
						private ArrayList<Protocal> lostMessages = new ArrayList<Protocal>();

						@Override
						protected ArrayList<Protocal> doInBackground(Object... params) {
							executing = true;
							try {
								if(IMCore.DEBUG)
									Log.d(TAG, "【QoS】=========== 消息发送质量保证线程运行中, 当前需要处理的列表长度为"+sentMessages.size()+"...");

								for(String key : sentMessages.keySet()) {
									final Protocal p = sentMessages.get(key);
									if(p != null && p.isQoS()) {
										if(p.getRetryCount() >= QOS_TRY_COUNT) {
											if(IMCore.DEBUG)
												Log.d(TAG, "【QoS】指纹为"+p.getFp()
														+"的消息包重传次数已达"+p.getRetryCount()+"(最多"+QOS_TRY_COUNT+"次)上限，将判定为丢包！");

											lostMessages.add((Protocal)p.clone());

											remove(p.getFp());
										}
										else {
											long delta = System.currentTimeMillis() - sendMessagesTimestamp.get(key);
											if(delta <= MESSAGES_JUST$NOW_TIME) {
												if(IMCore.DEBUG)
													Log.w(TAG, "【QoS】指纹为"+key+"的包距\"刚刚\"发出才"+delta
															+"ms(<="+MESSAGES_JUST$NOW_TIME+"ms将被认定是\"刚刚\"), 本次不需要重传哦.");
											}
											else {
												new LocalUDPDataSender.SendCommonDataAsync(context, p){
													@Override
													protected void onPostExecute(Integer code) {
														// 已成功重传
														if(code == 0) {
															// 重传次数+1
															p.increaseRetryCount();

															if(IMCore.DEBUG)
																Log.d(TAG, "【QoS】指 纹为"+p.getFp()
																		+"的消息包已成功进行重传，此次之后重传次数已达"
																		+p.getRetryCount()+"(最多"+QOS_TRY_COUNT+"次).");
														}
														else {
															Log.w(TAG, "【QoS】指纹为"+p.getFp()
																	+"的消息包重传失败，它的重传次数之前已累计为"
																	+p.getRetryCount()+"(最多"+QOS_TRY_COUNT+"次).");
														}
													}
												}.executeParallel();
											}
										}
									}
									else {
										remove(key);
									}
								}
							}
							catch (Exception eee) {
								Log.w(TAG, "【QoS】消息发送质量保证线程运行时发生异常,"+eee.getMessage(), eee);
							}
							return lostMessages;
						}

						@Override protected void onPostExecute(ArrayList<Protocal> al) {
							if(al != null && al.size() > 0)
								notifyMessageLost(al);

							executing = false;
							handler.postDelayed(runnable, CHECH_INTERVAL);
						}
					}.executeParallel();
				}
			}
		};
	}
}
