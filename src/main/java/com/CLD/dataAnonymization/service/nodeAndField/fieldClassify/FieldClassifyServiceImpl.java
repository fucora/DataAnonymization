package com.CLD.dataAnonymization.service.nodeAndField.fieldClassify;

import com.CLD.dataAnonymization.dao.h2.entity.*;
import com.CLD.dataAnonymization.dao.h2.repository.*;
import com.CLD.dataAnonymization.model.FieldFormInfo;
import com.CLD.dataAnonymization.model.FieldInfo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author CLD
 * @Date 2018/5/23 9:08
 **/
@Service
public class FieldClassifyServiceImpl implements FieldClassifyService {

    @Value("${field.out.path}")
    private String fieldPath;

    @Value("${package.jar.name}")
    private String jarName;

    private String FilePath_mapping=new Object() {
        public String get(){
            return this.getClass().getClassLoader().getResource("").getPath();
        }
    }.get().replaceAll("target/classes/","")
            .replaceAll(jarName+"!/BOOT-INF/classes!/","")
            .replaceAll("file:","");

    @Autowired
    UsageFieldClassifyRepository usageFieldClassifyRepository;

    @Autowired
    ArchetypeBasisFieldClassifyRepository archetypeBasisFieldClassifyRepository;

    @Autowired
    ExpandBasisFieldClassifyRepository expandBasisFieldClassifyRepository;

    @Autowired
    FieldClassifyListRepository fieldClassifyListRepository;

    @Autowired
    FieldClassifyRepository fieldClassifyRepository;

    @Autowired
    FieldClassifyUsageCountRepository fieldClassifyUsageCountRepository;

    @Autowired
    FieldChangeLogRepository fieldChangeLogRepository;

    @Override
    public List<String> createOrignalFrom() {
        return createOrignalFrom(archetypeBasisFieldClassifyRepository.findAll(),expandBasisFieldClassifyRepository.findAll());
    }


    @Override
    public List<String> createOrignalFrom(List<ArchetypeBasisFieldClassify> archetypeBasisFieldClassifyList,
                                          List<ExpandBasisFieldClassify> expandBasisFieldClassifyList) {
        if(archetypeBasisFieldClassifyList==null) archetypeBasisFieldClassifyList=archetypeBasisFieldClassifyRepository.findAll();
        if(expandBasisFieldClassifyList==null) expandBasisFieldClassifyList=expandBasisFieldClassifyRepository.findAll();
        Map<String,String> fieldMap=new HashMap<String, String>();
        Map<String,String> pathMap=new HashMap<String, String>();
        List<String> outList=new ArrayList<String>();
        for(ArchetypeBasisFieldClassify archetypeBasisFieldClassify:archetypeBasisFieldClassifyList){
            String fieldName=archetypeBasisFieldClassify.getFieldName();
            String fieldType=archetypeBasisFieldClassify.getFieldType();
            String fieldPath=archetypeBasisFieldClassify.getArchetypePath();
            if((fieldMap.get(fieldName)!=null)&& (!fieldMap.get(fieldName).equals(fieldType)))
                outList.add("表："+pathMap.get(fieldName)+";"+fieldPath+"字段："+fieldName+"冲突！");
                fieldMap.put(fieldName,fieldType);
                pathMap.put(fieldName,pathMap.get(fieldName)==null?fieldPath:pathMap.get(fieldName)+";"+fieldPath);
        }
        for(ExpandBasisFieldClassify expandBasisFieldClassify:expandBasisFieldClassifyList){
            String fieldName=expandBasisFieldClassify.getFieldName();
            String fieldType=expandBasisFieldClassify.getFieldType();
            String fieldPath=expandBasisFieldClassify.getExpandFromName();
            if((fieldMap.get(fieldName)!=null)&& (!fieldMap.get(fieldName).equals(fieldType)))
                outList.add("表："+pathMap.get(fieldName)+";"+fieldPath+"字段："+fieldName+"冲突！");
            fieldMap.put(fieldName,fieldType);
            pathMap.put(fieldName,pathMap.get(fieldName)==null?fieldPath:pathMap.get(fieldName)+";"+fieldPath);
        }
        if(outList.size()!=0) return outList;

        //保存original表
        usageFieldClassifyRepository.deleteByFromName("Original");
        for(String key:fieldMap.keySet()){
            UsageFieldClassify usageFieldClassify=new UsageFieldClassify();
            usageFieldClassify.setFieldName(key);
            usageFieldClassify.setFieldType(fieldMap.get(key));
            usageFieldClassify.setFromName("Original");
            usageFieldClassifyRepository.save(usageFieldClassify);
        }

        return outList;
    }

    @Override
    public List<String> getFromNameList() {
        return usageFieldClassifyRepository.getFromName();
    }

    @Override
    public List<String> getFromNameListByUserName(String userName){
        return fieldClassifyListRepository.getFormNameByUserName(userName);
    }

    @Override
    public List<FieldFormInfo> getFieldFormInfo(){
        List<FieldFormInfo> fieldFormInfoList =new ArrayList<FieldFormInfo>();
        List<FieldClassifyList> fieldClassifyListList=fieldClassifyListRepository.findAll();
        for(FieldClassifyList fieldClassifyList:fieldClassifyListList){
            FieldClassifyUsageCount fieldClassifyUsageCount=fieldClassifyUsageCountRepository.findByFormName(fieldClassifyList.getFormName());
            List<FieldChangeLog> fieldChangeLogList=fieldChangeLogRepository.findByFormName(fieldClassifyList.getFormName());
            Long createTime=Long.MAX_VALUE;
            Long lastChangeTime= Long.MIN_VALUE;
            for(FieldChangeLog fieldChangeLog:fieldChangeLogList){
                if(fieldChangeLog.getDateTime().getTime()<createTime) createTime=fieldChangeLog.getDateTime().getTime();
                if(fieldChangeLog.getDateTime().getTime()>lastChangeTime) lastChangeTime=fieldChangeLog.getDateTime().getTime();
            }
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
            FieldFormInfo fieldFormInfo =new FieldFormInfo();
            fieldFormInfo.setUserName(fieldClassifyList.getUserName());
            fieldFormInfo.setFormName(fieldClassifyList.getFormName());
            fieldFormInfo.setCreateTime(sdf.format(new Date(createTime)));
            fieldFormInfo.setLastChangeTime(sdf.format(new Date(lastChangeTime)));
            fieldFormInfo.setDescription(fieldClassifyList.getDescription());
            fieldFormInfo.setFather(fieldClassifyList.getFather());
            fieldFormInfo.setUsageCount(String.valueOf(fieldClassifyUsageCount.getCount()));

            fieldFormInfoList.add(fieldFormInfo);
        }
        return fieldFormInfoList;
    }

    @Override
    public FieldFormInfo getFieldFormInfoByFormName(String formName){
        FieldFormInfo fieldFormInfo=new FieldFormInfo();
        FieldClassifyList fieldClassifyList=fieldClassifyListRepository.findByFormName(formName);
        FieldClassifyUsageCount fieldClassifyUsageCount=fieldClassifyUsageCountRepository.findByFormName(formName);
        List<FieldChangeLog> fieldChangeLogList=fieldChangeLogRepository.findByFormName(formName);
        Long createTime=Long.MAX_VALUE;
        Long lastChangeTime= Long.MIN_VALUE;
        for(FieldChangeLog fieldChangeLog:fieldChangeLogList){
            if(fieldChangeLog.getDateTime().getTime()<createTime) createTime=fieldChangeLog.getDateTime().getTime();
            if(fieldChangeLog.getDateTime().getTime()>lastChangeTime) lastChangeTime=fieldChangeLog.getDateTime().getTime();
        }
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        fieldFormInfo.setUserName(fieldClassifyList.getUserName());
        fieldFormInfo.setFormName(fieldClassifyList.getFormName());
        fieldFormInfo.setCreateTime(sdf.format(new Date(createTime)));
        fieldFormInfo.setLastChangeTime(sdf.format(new Date(lastChangeTime)));
        fieldFormInfo.setDescription(fieldClassifyList.getDescription());
        fieldFormInfo.setFather(fieldClassifyList.getFather());
        fieldFormInfo.setUsageCount(String.valueOf(fieldClassifyUsageCount.getCount()));
        return fieldFormInfo;
    }


    @Override
    public List<FieldInfo> getFieldByFromName(String fromName) {
        List<UsageFieldClassify> usageFieldClassifyList = usageFieldClassifyRepository.findByFromName(fromName);
        List<FieldInfo> fieldInfoList=new ArrayList<FieldInfo>();
        for(UsageFieldClassify usageFieldClassify : usageFieldClassifyList){
            FieldInfo fieldInfo=new FieldInfo();
            fieldInfo.setFieldName(usageFieldClassify.getFieldName());
            fieldInfo.setFieldType(usageFieldClassify.getFieldType());
            fieldInfo.setId(String.valueOf(usageFieldClassify.getID()));
            fieldInfoList.add(fieldInfo);
        }
        return fieldInfoList;
    }

    @Override
    public ArrayList<ArrayList<String>> getUseFieldByFromName(String fromName) {
        List<FieldInfo> fieldInfoList=getFieldByFromName(fromName);
        ArrayList<ArrayList<String>> fieldList=new ArrayList<ArrayList<String>>();
        for(FieldInfo fieldInfo:fieldInfoList){
            ArrayList<String> field=new ArrayList<String>();
            field.add(fieldInfo.getFieldName());
            field.add(fieldInfo.getFieldType());
            fieldList.add(field);
        }
        return fieldList;
    }

    @Override
    public List<String> updataField(List<FieldInfo> fieldInfoList, String newFromName) {
        List<String> outList=new ArrayList<String>();

        //检验字段是否可行(不能有重复字段)
        Set<String> fields=new HashSet<String>();
        for(FieldInfo fieldInfo:fieldInfoList){
            if(fields.contains(fieldInfo.getFieldName())) outList.add("字段"+fieldInfo.getFieldName()+"冲突/r/n");
            else fields.add(fieldInfo.getFieldName());
        }
        if(outList.size()!=0) return outList;
        //
        for(FieldInfo fieldInfo:fieldInfoList){
            if(fieldInfo.getFieldName()==null||fieldInfo.getFieldType()==null) continue;
            UsageFieldClassify usageFieldClassify =new UsageFieldClassify();
            usageFieldClassify.setFromName(newFromName);
            usageFieldClassify.setFieldName(fieldInfo.getFieldName());
            usageFieldClassify.setFieldType(fieldInfo.getFieldType());
            usageFieldClassifyRepository.save(usageFieldClassify);
        }
        outList.add("更新成功！");
        return outList;
    }

    @Transactional
    @Override
    public List<String> updateFieldFormInfo(List<FieldInfo> fieldInfoList, String newFormName,String oldFormName,String newDescription,String logDescription){
        List<String> outList=new ArrayList<String>();
        try {

            //判断新表名是否可用
            if(!newFormName.equals(oldFormName)){
                List<String> formNameList=fieldClassifyListRepository.getFormName();
                if(formNameList.contains(newFormName)){
                    outList.add("表名已存在！");
                    return outList;
                }
            }
            //判断字段是否冲突
            Map<String,String> fieldMap=new HashMap<String,String>();
            for(FieldInfo fieldInfo:fieldInfoList){
                String fieldName=fieldInfo.getFieldName()
                        .toLowerCase()
                        .replace(".","")
                        .replace("_","")
                        .replace("-","")
                        .replace("*","");
                if(fieldMap.keySet().contains(fieldName)&&!fieldMap.get(fieldName).equals(fieldInfo.getFieldType()))
                    outList.add("字段"+fieldInfo.getFieldName()+"冲突/r/n");
                else fieldMap.put(fieldName,fieldInfo.getFieldType());
            }
            if(outList.size()!=0) return outList;
            //存fieldChangeLog
            String log="";
            FieldChangeLog fieldChangeLog=new FieldChangeLog();
            fieldChangeLog.setDescription(logDescription);
            fieldChangeLog.setDateTime(new java.sql.Date(new Date().getTime()));
            fieldChangeLog.setFormName(newFormName);
            if(!newFormName.equals(oldFormName))
                log+="重命名："+oldFormName+"-->"+newFormName+"/r/n";
            List<FieldClassify> oldFieldClassify=fieldClassifyRepository.findByFormName(oldFormName);
            Set<String> fieldSet=new HashSet<String>(fieldMap.keySet());
            for(FieldClassify fieldClassify:oldFieldClassify){
                if(!fieldMap.keySet().contains(fieldClassify.getFieldName()))
                    log+="移除字段："+fieldClassify.getFieldName()+"/r/n";
                else if(fieldMap.keySet().contains(fieldClassify.getFieldName())&&!fieldMap.get(fieldClassify.getFieldName()).equals(fieldClassify.getFieldType()))
                    log+="改变字段："+fieldClassify.getFieldName()+" "+fieldClassify.getFieldType()+"-->"+fieldMap.get(fieldClassify.getFieldName())+"/r/n";
                else
                    fieldSet.remove(fieldClassify.getFieldName());
            }
            if(fieldSet.size()!=0){
                log+="增加字段：";
                for(String s:fieldSet)
                    log+=s+"  ";
            }
            fieldChangeLog.setChangeLog(log);
            fieldChangeLogRepository.save(fieldChangeLog);
            List<FieldChangeLog> fieldChangeLogList=fieldChangeLogRepository.findByFormName(oldFormName);
            fieldChangeLogRepository.deleteByFormName(oldFormName);
            for(FieldChangeLog fieldChangeLog1:fieldChangeLogList)
                fieldChangeLog1.setFormName(newFormName);
            fieldChangeLogRepository.flush();
            fieldChangeLogRepository.saveAll(fieldChangeLogList);
            //存fieldClassify
            List<FieldClassify> fieldClassifyList=new ArrayList<FieldClassify>();
            for(String s:fieldMap.keySet()){
                FieldClassify fieldClassify=new FieldClassify();
                fieldClassify.setFieldName(s);
                fieldClassify.setFieldType(fieldMap.get(s));
                fieldClassify.setFormName(newFormName);
                fieldClassifyList.add(fieldClassify);
            }
            fieldClassifyRepository.deleteByFormName(oldFormName);
            fieldClassifyRepository.flush();
            fieldClassifyRepository.saveAll(fieldClassifyList);
            //存fieldClassfyList
            FieldClassifyList fieldClassifyList1=fieldClassifyListRepository.findByFormName(oldFormName);
            fieldClassifyList1.setDescription(newDescription);
            fieldClassifyList1.setFormName(newFormName);
            fieldClassifyListRepository.save(fieldClassifyList1);
            //存fieldUsageCount
            FieldClassifyUsageCount fieldClassifyUsageCount=fieldClassifyUsageCountRepository.findByFormName(oldFormName);
            fieldClassifyUsageCountRepository.deleteByFormName(oldFormName);
            fieldClassifyUsageCount.setFormName(newFormName);
            fieldClassifyUsageCountRepository.flush();
            fieldClassifyUsageCountRepository.save(fieldClassifyUsageCount);
        }catch (Exception e){
            e.printStackTrace();
            outList.add("更新失败！");
            return outList;
        }
        outList.add("更新成功！");
        return outList;
    }

    @Override
    public Boolean deleteFromByName(String formName) {
        try {
            usageFieldClassifyRepository.deleteByFromName(formName);
            fieldClassifyListRepository.deleteByFormName(formName);
            fieldClassifyRepository.deleteByFormName(formName);
            fieldChangeLogRepository.deleteByFormName(formName);
            fieldClassifyUsageCountRepository.deleteByFormName(formName);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    @Override
    public Boolean deleteFormByFormNameAndUserName(String formName,String userName){
        try {
            fieldClassifyListRepository.deleteByFormNameAndUserName(formName,userName);
            fieldClassifyRepository.deleteByFormName(formName);
            fieldChangeLogRepository.deleteByFormName(formName);
            fieldClassifyUsageCountRepository.deleteByFormName(formName);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public Map<String,Double> getFieldOverViewByFormName(String formName){
        Map<String,Double> map=new HashMap<String,Double>();
        List<FieldClassify> fieldClassifyList=fieldClassifyRepository.findByFormName(formName);
        Double EI= Double.valueOf(0);
        Double QI_Link= Double.valueOf(0);
        Double QI_Geography= Double.valueOf(0);
        Double QI_DateRecord= Double.valueOf(0);
        Double QI_DateAge= Double.valueOf(0);
        Double QI_Number= Double.valueOf(0);
        Double QI_String= Double.valueOf(0);
        Double SI_Number= Double.valueOf(0);
        Double SI_String= Double.valueOf(0);
        Double UI=Double.valueOf(0);
        Double sum= Double.valueOf(0);
        for(FieldClassify fieldClassify:fieldClassifyList){
            if(fieldClassify.getFieldType().equals("EI")) EI++;
            if(fieldClassify.getFieldType().equals("QI_Link")) QI_Link++;
            if(fieldClassify.getFieldType().equals("QI_Geography")) QI_Geography++;
            if(fieldClassify.getFieldType().equals("QI_DateRecord")) QI_DateRecord++;
            if(fieldClassify.getFieldType().equals("QI_DateAge")) QI_DateAge++;
            if(fieldClassify.getFieldType().equals("QI_Number")) QI_Number++;
            if(fieldClassify.getFieldType().equals("QI_String")) QI_String++;
            if(fieldClassify.getFieldType().equals("SI_Number")) SI_Number++;
            if(fieldClassify.getFieldType().equals("SI_String")) SI_String++;
            if(fieldClassify.getFieldType().equals("UI")) UI++;
        }
        sum=EI+QI_DateAge+QI_DateRecord+QI_Geography+QI_Link+QI_Number+QI_String+SI_Number+SI_String+UI;
        map.put("EI",(Double)(EI/sum));
        map.put("QI_Link",(Double)(QI_Link/sum));
        map.put("QI_Geography",(Double)(QI_Geography/sum));
        map.put("QI_DateRecord",(Double)(QI_DateRecord/sum));
        map.put("QI_DateAge",(Double)(QI_DateAge/sum));
        map.put("QI_Number",(Double)(QI_Number/sum));
        map.put("QI_String",(Double)(QI_String/sum));
        map.put("SI_Number",(Double)(SI_Number/sum));
        map.put("SI_String",(Double)(SI_String/sum));
        map.put("UI",(Double)(UI/sum));
        map.put("SUM",sum);
        return map;
    }

    @Override
    public Map<String,List<String>> getFieldDetailByFormName(String formName){
        Map<String,List<String>> map=new HashMap<String,List<String>>();
        map.put("EI",new ArrayList<String>());
        map.put("QI_Link",new ArrayList<String>());
        map.put("QI_Geography",new ArrayList<String>());
        map.put("QI_DateRecord",new ArrayList<String>());
        map.put("QI_DateAge",new ArrayList<String>());
        map.put("QI_Number",new ArrayList<String>());
        map.put("QI_String",new ArrayList<String>());
        map.put("SI_Number",new ArrayList<String>());
        map.put("SI_String",new ArrayList<String>());
        map.put("UI",new ArrayList<String>());
        List<FieldClassify> fieldClassifyList=fieldClassifyRepository.findByFormName(formName);
        for(FieldClassify fieldClassify:fieldClassifyList){
            if(fieldClassify.getFieldType().equals("EI")) map.get("EI").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("QI_Link")) map.get("QI_Link").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("QI_Geography")) map.get("QI_Geography").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("QI_DateRecord")) map.get("QI_DateRecord").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("QI_DateAge")) map.get("QI_DateAge").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("QI_Number")) map.get("QI_Number").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("QI_String")) map.get("QI_String").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("SI_Number")) map.get("SI_Number").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("SI_String")) map.get("SI_String").add(fieldClassify.getFieldName());
            if(fieldClassify.getFieldType().equals("UI")) map.get("UI").add(fieldClassify.getFieldName());
        }
        return map;
    }


    @Override
    @Transactional
    public Boolean createFrom(String formName,String father,String userName,String description){
        try{
            //
            FieldClassifyList fieldClassifyList=new FieldClassifyList();
            fieldClassifyList.setFormName(formName);
            fieldClassifyList.setFather(father);
            fieldClassifyList.setDescription(description);
            fieldClassifyList.setUserName(userName);
            fieldClassifyListRepository.save(fieldClassifyList);

            //
            FieldChangeLog fieldChangeLog=new FieldChangeLog();
            fieldChangeLog.setDescription("创建表单");
            fieldChangeLog.setFormName(formName);
            fieldChangeLog.setDateTime(new java.sql.Date(new Date().getTime()));
            fieldChangeLog.setChangeLog("创建表单");
            fieldChangeLogRepository.save(fieldChangeLog);

            //
            FieldClassifyUsageCount fieldClassifyUsageCount=new FieldClassifyUsageCount();
            fieldClassifyUsageCount.setFormName(formName);
            fieldClassifyUsageCount.setCount(0);
            fieldClassifyUsageCountRepository.save(fieldClassifyUsageCount);

            //
            List<FieldClassify> fieldClassifyList1=fieldClassifyRepository.findByFormName(father);
            List<FieldClassify> newFieldClassifyList=new ArrayList<FieldClassify>();
            for(FieldClassify fieldClassify:fieldClassifyList1){
                FieldClassify newfieldClassify=new FieldClassify();
                newfieldClassify.setFormName(formName);
                newfieldClassify.setFieldType(fieldClassify.getFieldType());
                newfieldClassify.setFieldName(fieldClassify.getFieldName());
                newFieldClassifyList.add(newfieldClassify);
            }
            fieldClassifyRepository.saveAll(newFieldClassifyList);

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }


    @Override
    public Boolean FileToDB() {
        InputStream is= null;
        File file=new File(FilePath_mapping+fieldPath);
        String[] fileList = file.list();
        for (int i = 0; i < fileList.length; i++) {
            if(fileList[i].equals("Original.json")) continue;
            usageFieldClassifyRepository.deleteByFromName(fileList[i].split("\\.")[0]);
            JSONArray jsonArray=new JSONArray();
            String path=FilePath_mapping+fieldPath+"/"+fileList[i];
            try {
                is = new FileInputStream(path);
                JSONReader reader=new JSONReader(new InputStreamReader(is,"UTF-8"));
                reader.startArray();
                while(reader.hasNext()) {
                    JSONObject ja= (JSONObject) reader.readObject();
                    jsonArray.add(ja);
                }
                reader.endArray();
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }
            for(int j=0;j<jsonArray.size();j++){
                UsageFieldClassify usageFieldClassify=new UsageFieldClassify();
                usageFieldClassify.setFromName(fileList[i].split("\\.")[0]);
                usageFieldClassify.setFieldName(jsonArray.getJSONObject(j).getString("fieldName"));
                usageFieldClassify.setFieldType(jsonArray.getJSONObject(j).getString("fieldType"));
                usageFieldClassifyRepository.save(usageFieldClassify);
            }
        }
        return true;
    }

    @Override
    public Boolean DBToFile() {
        List<String> fieldNameList=usageFieldClassifyRepository.getFromName();
        for(String fieldName:fieldNameList) {
            List<UsageFieldClassify> usageFieldClassifyList = usageFieldClassifyRepository.findByFromName(fieldName);
            JSONArray jsonArray=new JSONArray();
            for(UsageFieldClassify usageFieldClassify:usageFieldClassifyList){
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("fieldName",usageFieldClassify.getFieldName());
                jsonObject.put("fieldType",usageFieldClassify.getFieldType());
                jsonArray.add(jsonObject);
            }
            String jsonStr =jsonArray.toJSONString();
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new BufferedWriter(new FileWriter(FilePath_mapping+fieldPath+"/backup"+fieldName+".json")));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            pw.print(jsonStr);
            pw.flush();
            pw.close();
        }
        return true;
    }


}
