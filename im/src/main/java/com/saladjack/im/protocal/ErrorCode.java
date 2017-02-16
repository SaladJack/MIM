/*
 * Copyright (C) 2016 即时通讯网(52im.net) The MobileIMSDK Project. 
 * All rights reserved.
 * Project URL:https://github.com/JackJiang2011/MobileIMSDK
 *  
 * 即时通讯网(52im.net) - 即时通讯技术社区! PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 * 
 * ErrorCode.java at 2016-2-20 11:26:02, code by Jack Jiang.
 * You can contact author with jack.jiang@52im.net or jb2011@163.com.
 */
package com.saladjack.im.protocal;

/**
 * 错误码常量表.
 * <b>建议0~1024范围内的错误码作为Im核心框架保留，业务层请使用>1024的码表示！
 *
 * @author Jack Jiang, 2013-11
 * @version 1.0
 */
public interface ErrorCode
{
	/** 一切正常 */
	int COMMON_CODE_OK = 0;
	/** 客户端尚未登陆 */
	int COMMON_NO_LOGIN = 1;
	/** 未知错误 */
	int COMMON_UNKNOW_ERROR = 2;

	/** 数据发送失败 */
	int COMMON_DATA_SEND_FAILD = 3;

	/** 无效的 {@link Protocal}对象 */
	int COMMON_INVALID_PROTOCAL = 4;

	/** 由客户端产生的错误码 */
	interface ForC
	{
		/** 与服务端的连接已断开 */
		int BREOKEN_CONNECT_TO_SERVER = 201;

		/** 与服务端的网络连接失败 */
		int BAD_CONNECT_TO_SERVER = 202;

		/** 客户端SDK尚未初始化 */
		int CLIENT_SDK_NO_INITIALED = 203;

		/** 本地网络不可用（未打开） */
		int LOCAL_NETWORK_NOT_WORKING = 204;

		/** 要连接的服务端网络参数未设置 */
		int TO_SERVER_NET_INFO_NOT_SETUP = 205;
	}

	/** 由服务端产生的错误码 */
	interface ForS
	{
		/** 客户端尚未登陆，请重新登陆 */
		int RESPONSE_FOR_UNLOGIN = 301;
		int LOGIN_FAIL = 1025;
		int REGISTER_FAIL$ACCOUNT_ALREADY_EXIST = 1026;
	}

}