package com.CLD.dataAnonymization.web;

import com.CLD.dataAnonymization.dao.h2.entity.FieldChangeLog;
import com.CLD.dataAnonymization.dao.h2.repository.FieldChangeLogRepository;
import com.CLD.dataAnonymization.dao.h2.repository.FieldClassifyListRepository;
import com.CLD.dataAnonymization.model.FieldFormInfo;
import com.CLD.dataAnonymization.model.FieldInfo;
import com.CLD.dataAnonymization.service.nodeAndField.fieldClassify.FieldClassifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * @description:
 * @Author CLD
 * @Date 2018/8/17 16:04
 */
@Controller
@RequestMapping("/MyFieldForm")
public class MyFieldFormController {

    @Autowired
    FieldClassifyListRepository fieldClassifyListRepository;

    @Autowired
    FieldClassifyService fieldClassifyService;

    @Autowired
    FieldChangeLogRepository fieldChangeLogRepository;

    @RequestMapping("/MyFieldForm")
    public String myFieldForm(){
        return "MyFieldForm";
    }


    @RequestMapping("/getFieldFormName")
    @ResponseBody
    public List<String> getFieldFormName(){
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return fieldClassifyListRepository.getFormNameByUserName(userDetails.getUsername());
    }

    @RequestMapping("/getFieldFormInfoByFormName")
    @ResponseBody
    public FieldFormInfo getFieldFormInfoByFormName(@Param("formName") String formName){
        FieldFormInfo fieldFormInfo=fieldClassifyService.getFieldFormInfoByFormName(formName);
        return fieldFormInfo;
    }


    @RequestMapping("/getFieldDetailByFormName")
    @ResponseBody
    public Map<String,List<String>> getFieldDetailByFormName(@Param("formName")String formName){
        return fieldClassifyService.getFieldDetailByFormName(formName);
    }

    @RequestMapping("/getFieldOverViewByFormName")
    @ResponseBody
    public Map<String,Double> getFieldOverViewByFormName(@Param("formName")String formName){
        return fieldClassifyService.getFieldOverViewByFormName(formName);
    }

    @RequestMapping("/getFieldChangeLogByFormName")
    @ResponseBody
    public List<FieldChangeLog> getFieldChangeLogByFormName(@Param("formName")String formName){
        return fieldChangeLogRepository.findByFormName(formName);
    }

    @RequestMapping("/deleteFieldFormByFormName")
    @ResponseBody
    public Boolean deleteFieldFormByFormName(@Param("formName") String formName){
        try{
            String userName="";
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();
            userName=userDetails.getUsername();
            fieldClassifyService.deleteFormByFormNameAndUserName(formName,userName);
            return true;
        }catch(Exception e){
            return  false;
        }
    }

    @RequestMapping("/createForm")
    @ResponseBody
    public List<String> createForm(@Param("formName")String formName,
                                   @Param("father")String father,
                                   @Param("description")String description){
        List<String> outInfo=new ArrayList<String>();
        Set<String> formNameSet=new HashSet<String>(fieldClassifyListRepository.getFormName());
        if(formNameSet.contains(formName)){
            outInfo.add("表名已存在！");
            return outInfo;
        }
        if(!formNameSet.contains(father)){
            outInfo.add("模板不存在！");
            return outInfo;
        }
        String userName="";
        try{
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();
            userName=userDetails.getUsername();
        }catch(Exception e){
            outInfo.add("非法用户名！");
            return  outInfo;
        }
        if(fieldClassifyService.createFrom(formName,father,userName,description)==true){
            outInfo.add("添加成功！");
            return outInfo;
        }else {
            outInfo.add("添加失败！");
            return outInfo;
        }
    }

    @RequestMapping(value = "/updateFieldForm",method = RequestMethod.POST)
    @ResponseBody
    public List<String> updateFieldForm(@Param("newFormName") String newFormName,
                                        @Param("oldFormName") String oldFormName,
                                        @Param("newDescription") String newDescription,
                                        @Param("logDescription") String logDescription,
                                        @RequestBody List<FieldInfo> fieldInfoList){
        return fieldClassifyService.updateFieldFormInfo(fieldInfoList,newFormName,oldFormName,newDescription,logDescription);
    }
}