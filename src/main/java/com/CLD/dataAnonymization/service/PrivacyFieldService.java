package com.CLD.dataAnonymization.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * 该类用于定时更新敏感字段
 * @Author CLD
 * @Date 2018/5/10 9:27
 **/
public interface PrivacyFieldService {


    /**
     * 该方法用于获取按照原型分类的敏感字段
     * @return
     */
    public JSONArray getPrivaryFields() throws FileNotFoundException, UnsupportedEncodingException;


    /**
     * 此方法用于获取整理后的匿名字段
     * 按照属性21种分类
     * @return
     */
    public JSONObject getOrganizedFields() throws FileNotFoundException, UnsupportedEncodingException;

    /**
     * 此方法用于获取用于处理的匿名字段
     * 按照处理要求6种分类
     * @param
     * @return
     */
    public JSONObject getProcessingFields() throws FileNotFoundException, UnsupportedEncodingException;

    /**
     * 此方法用于更新匿名字段映射表
     * @param jsonArray
     * @return
     */
    public ArrayList<String> updataFieldFile(JSONArray jsonArray) throws UnsupportedEncodingException, FileNotFoundException;


    /**
     * 该方法通过clever接口，增加新的字段
     * @return
     */
    public Boolean pollField() throws FileNotFoundException, UnsupportedEncodingException;
}
