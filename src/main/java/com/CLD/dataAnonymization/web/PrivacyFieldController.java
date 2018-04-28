package com.CLD.dataAnonymization.web;

import com.CLD.dataAnonymization.service.DataParseService;
import com.CLD.dataAnonymization.service.PrivacyFieldService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 该控制器用于控制隐私字段的查询，修改。
 * @Author CLD
 * @Date 2018/4/2 14:24
 **/
@Controller
public class PrivacyFieldController {

    @Autowired
    DataParseService dataParseService;

    @Autowired
    private PrivacyFieldService privacyFieldService;

    ReadWriteLock readWriteLock=new ReentrantReadWriteLock();

    @RequestMapping(value = "/PrivacyFieldModify",method = RequestMethod.GET)
    public String viewOfPrivacyFieldModify(){
        return "PrivacyFieldModify";
    }

    @RequestMapping(value = "/PrivacyFieldOverView",method = RequestMethod.GET)
    public String viewOfPrivacyFieldOverView(){
        return "PrivacyFieldOverView";
    }

    /**
     * 提供完整的匿名字段表
     * @param request
     * @return
     * @throws FileNotFoundException
     */
    @RequestMapping(value = "/getPrivacyField",method = RequestMethod.GET)
    @ResponseBody
    public JSONArray getPrivacyField(HttpServletRequest request) throws  FileNotFoundException {
        readWriteLock.readLock().lock();
        JSONArray jsonArray = null;
        try {
            jsonArray = privacyFieldService.getPrivaryFields();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            readWriteLock.readLock().unlock();
            return jsonArray;
        }
    }

    /**
     * 提供整合过的匿名字段表
     * @return
     * @throws FileNotFoundException
     */
    @RequestMapping(value = "/getFieldOverView",method = RequestMethod.GET)
    @ResponseBody
    public JSONObject getFieldOverView() throws FileNotFoundException {
        readWriteLock.readLock().lock();
        JSONObject jsonObject=null;
        try {
            jsonObject = privacyFieldService.getOrganizedFields();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            readWriteLock.readLock().unlock();
            return jsonObject;
        }
    }

    @RequestMapping(value = "/updataFields",method = RequestMethod.POST)
    @ResponseBody
    public  ArrayList<String> updataFields(@RequestBody JSONArray jsonArray) throws  UnsupportedEncodingException{
        readWriteLock.writeLock().lock();
        ArrayList<String> arrayList=new ArrayList<String>();
        try{
            arrayList=privacyFieldService.updataFieldFile(jsonArray);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return arrayList;
    }
}
