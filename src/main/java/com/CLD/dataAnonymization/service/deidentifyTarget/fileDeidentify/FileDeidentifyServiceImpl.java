package com.CLD.dataAnonymization.service.deidentifyTarget.fileDeidentify;

import com.CLD.dataAnonymization.dao.h2.entity.FieldClassifyUsageCount;
import com.CLD.dataAnonymization.dao.h2.repository.FieldClassifyUsageCountRepository;
import com.CLD.dataAnonymization.model.AnonymizeConfigure;
import com.CLD.dataAnonymization.model.FieldInfo;
import com.CLD.dataAnonymization.service.nodeAndField.fieldClassify.FieldClassifyService;
import com.CLD.dataAnonymization.util.deidentifier.Anonymizer;
import com.CLD.dataAnonymization.util.deidentifier.Configuration;
import com.CLD.dataAnonymization.util.deidentifier.DataHandle;
import com.CLD.dataAnonymization.util.deidentifier.io.FileResolve;
import com.CLD.dataAnonymization.util.deidentifier.io.ZipCompressor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * 该类用于处理文件类型数据
 * @Author CLD
 * @Date 2018/4/16 8:43
 **/
@Service
public class FileDeidentifyServiceImpl implements FileDeidentifyService {

    @Autowired
    FieldClassifyService fieldClassifyService;

    @Autowired
    FieldClassifyUsageCountRepository fieldClassifyUsageCountRepository;

    @Override
    public String FileDeidentify(MultipartHttpServletRequest re, HttpServletRequest rq) throws Exception {
        //获得配置文件
        Map<String,String[]> parameterMap=re.getParameterMap();
        AnonymizeConfigure anonymizeConfigure=new AnonymizeConfigure();
        anonymizeConfigure.setFieldFormName(parameterMap.get("fieldFormName")[0]);
        anonymizeConfigure.setLevel(parameterMap.get("level")[0]);
        anonymizeConfigure.setEncryptPassword(parameterMap.get("encryptPassword")[0]);
        anonymizeConfigure.setK_big(parameterMap.get("k_big")[0]);
        anonymizeConfigure.setK_small(parameterMap.get("k_small")[0]);
        anonymizeConfigure.setMicroaggregation(parameterMap.get("microaggregation")[0]);
        anonymizeConfigure.setNoiseScope_big(parameterMap.get("noiseScope_big")[0]);
        anonymizeConfigure.setNoiseScope_small(parameterMap.get("noiseScope_small")[0]);
        anonymizeConfigure.setSuppressionLimit_level1(parameterMap.get("suppressionLimit_level1")[0]);
        anonymizeConfigure.setSuppressionLimit_level2(parameterMap.get("suppressionLimit_level2")[0]);
        anonymizeConfigure.setT(parameterMap.get("t")[0]);
        //准备匿名字段表
        List<FieldInfo> fieldInfoList=fieldClassifyService.getFieldByFromName(anonymizeConfigure.getFieldFormName());
        ArrayList<ArrayList<String>> fieldList=new ArrayList<ArrayList<String>>();
        for(FieldInfo fieldInfo:fieldInfoList){
            ArrayList<String> field=new ArrayList<String>();
            field.add(fieldInfo.getFieldName());
            field.add(fieldInfo.getFieldType());
            fieldList.add(field);
        }

        //准备文件暂存路径
        String time=String.valueOf(System.currentTimeMillis());
        String savePath =rq.getSession().getServletContext().getRealPath("/identityFiles/"+time);
        FileResolve.createFile(savePath);

        //匿名化配置
        Configuration configuration=new Configuration();
        if(anonymizeConfigure.getLevel().equals("Level1"))
            configuration.setLevel(Configuration.AnonymousLevel.Level1);
        else
            configuration.setLevel(Configuration.AnonymousLevel.Level2);
        configuration.setEncryptPassword(anonymizeConfigure.getEncryptPassword());
        configuration.setK_big(Integer.valueOf(anonymizeConfigure.getK_big()));
        configuration.setK_small(Integer.valueOf(anonymizeConfigure.getK_small()));
        configuration.setMicroaggregation(Integer.valueOf(anonymizeConfigure.getMicroaggregation()));
        configuration.setNoiseScope_big(Double.valueOf(anonymizeConfigure.getNoiseScope_big()));
        configuration.setNoiseScope_small(Double.valueOf(anonymizeConfigure.getNoiseScope_small()));
        configuration.setSuppressionLimit_level1(Double.valueOf(anonymizeConfigure.getSuppressionLimit_level1()));
        configuration.setSuppressionLimit_level2(Double.valueOf(anonymizeConfigure.getSuppressionLimit_level2()));
        configuration.setT(Double.valueOf(anonymizeConfigure.getT()));

        //文件处理
        MultiValueMap<String, MultipartFile> files=re.getMultiFileMap();
        for(String key:files.keySet()) {
            MultipartFile file = files.getFirst(key);
            String filename = file.getOriginalFilename();
            if(filename.endsWith(".csv")){
                DataHandle dataHandle=new DataHandle(FileResolve.readerCsv(file.getInputStream()));
                dataHandle.setFieldList(fieldList);
                Anonymizer anonymizer=new Anonymizer(dataHandle,configuration);
                anonymizer.anonymize();
                FileResolve.writerCsv(savePath+"\\"+anonymizeConfigure.getLevel()+filename,dataHandle.getData());
            }
            if(filename.endsWith(".xls")){
                ArrayList<ArrayList<ArrayList<String>>> data=FileResolve.readerXls(file.getInputStream());
                for (int i = 0; i <data.size()-1 ; i++){
                    DataHandle dataHandle=new DataHandle(data.get(i));
                    dataHandle.setFieldList(fieldList);
                    Anonymizer anonymizer=new Anonymizer(dataHandle,configuration);
                    anonymizer.anonymize();
                    data.set(i,dataHandle.getData());
                }
                FileResolve.writerXlsx(savePath+"\\"+anonymizeConfigure.getLevel()+filename+"x",data);
            }
            if(filename.endsWith(".xlsx")){
                ArrayList<ArrayList<ArrayList<String>>> data=FileResolve.readerXlsx(file.getInputStream());
                for (int i = 0; i <data.size()-1 ; i++){
                    DataHandle dataHandle=new DataHandle(data.get(i));
                    dataHandle.setFieldList(fieldList);
                    Anonymizer anonymizer=new Anonymizer(dataHandle,configuration);
                    anonymizer.anonymize();
                    data.set(i,dataHandle.getData());
                }
                FileResolve.writerXlsx(savePath+"\\"+anonymizeConfigure.getLevel()+filename,data);
            }
        }

        //处理文件打包存储
        ZipCompressor zc = new ZipCompressor(savePath+".zip");
        zc.compress(savePath);
        FileResolve.deleteFile(savePath);
        //延时1天清空zip 文件
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    FileResolve.deleteFile(savePath+".zip");
                } catch (UnsupportedEncodingException e) {e.printStackTrace();}
            }
        };
        Timer timer = new Timer();
        long delay = 24*60*60*1000;
        timer.schedule(task, delay);

        //表单使用记录
        FieldClassifyUsageCount fieldClassifyUsageCount=fieldClassifyUsageCountRepository.findByFormName(anonymizeConfigure.getFieldFormName());
        fieldClassifyUsageCount.setCount(fieldClassifyUsageCount.getCount()+1);
        fieldClassifyUsageCountRepository.save(fieldClassifyUsageCount);

        return "/identityFiles/"+time+".zip";
    }

    @Override
    public AnonymizeConfigure getAnonymizeConfigure() {
        AnonymizeConfigure anonymizeConfigure=new AnonymizeConfigure();
        Configuration configuration=new Configuration();
        anonymizeConfigure.setEncryptPassword(configuration.getEncryptPassword());
        anonymizeConfigure.setFieldFormName("");
        anonymizeConfigure.setK_big(String.valueOf(configuration.getK_big()));
        anonymizeConfigure.setK_small(String.valueOf(configuration.getK_small()));
        anonymizeConfigure.setLevel(String.valueOf(configuration.getLevel()));
        anonymizeConfigure.setMicroaggregation(String.valueOf(configuration.getMicroaggregation()));
        anonymizeConfigure.setNoiseScope_big(String.valueOf(configuration.getNoiseScope_big()));
        anonymizeConfigure.setNoiseScope_small(String.valueOf(configuration.getNoiseScope_small()));
        anonymizeConfigure.setSuppressionLimit_level1(String.valueOf(configuration.getSuppressionLimit_level1()));
        anonymizeConfigure.setSuppressionLimit_level2(String.valueOf(configuration.getSuppressionLimit_level2()));
        anonymizeConfigure.setT(String.valueOf(configuration.getT()));
        return anonymizeConfigure;
    }

}
