package com.gs.gscartoon.realm;


import com.gs.gscartoon.history.bean.HistoryBean;
import com.gs.gscartoon.utils.AppConstants;
import com.gs.gscartoon.utils.LogUtil;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by yinjianfeng on 16/12/6.
 */

public class DataHelper {
    public final static String TAG = "DataHelper";

    //realm数据库名字
    public final static String REALM_NAME = "GsCartoonRealm.realm";

    private static boolean isHistoryLimit = false;//是否达到历史记录的上线

    /**
     * 得到数据库实例
     * @return
     */
    public static Realm getRealmInstance(){
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name(DataHelper.REALM_NAME)
                .schemaVersion(Migration.REALM_VERSION) // Must be bumped when the schema changes
                .migration(new Migration()) // Migration to run instead of throwing an exception
                .build();

        Realm realm = null;
        try {
            realm = Realm.getInstance(config);
        } catch (Exception e) {
            LogUtil.e(TAG,"Realm数据库出错："+e.getMessage());
            return realm;
        }
        return realm;
    }

    /**
     * 历史记录
     * 根据id号获取一条数据，如果没有数据可以在数据库中插入一条数据。子线程中不能访问！！！
     * isCreate,没有数据时是否创建
     * @return
     */
    public static HistoryBean getHistoryById(String id, boolean isCreate){
        Realm realm = getRealmInstance();
        HistoryBean bean = null;
        RealmResults<HistoryBean> result = realm.where(HistoryBean.class).equalTo("id", id)
                .findAll();
        if(result.size() == 0){
            if(isCreate) {
                if(isHistoryLimit){//达到历史记录上限
                    LogUtil.e(TAG, "达到历史记录上限");
                    deleteOldestHistory();
                }else {
                    LogUtil.e(TAG, "没有达到历史记录上限");
                    if(realm.where(HistoryBean.class).findAll().size() >= AppConstants.HISTORY_LIMIT){
                        LogUtil.e(TAG, "达到历史记录上限");
                        isHistoryLimit = true;
                        deleteOldestHistory();
                    }
                }

                bean = new HistoryBean();
                bean.setId(id);

                realm.beginTransaction();
                bean = realm.copyToRealmOrUpdate(bean);
                realm.commitTransaction();
            }
        }else {
            bean = result.first();
        }
        realm.close();
        return bean;
    }

    /**
     * 删除最旧的历史记录
     * @return
     */
    public static boolean deleteOldestHistory(){
        Realm realm = getRealmInstance();
        HistoryBean bean;
        RealmResults<HistoryBean> realmResults = realm
                .where(HistoryBean.class).findAll().sort("updateTime", Sort.ASCENDING);
        if(realmResults.size() == 0){
            return false;
        }else {
            bean = realmResults.first();

            realm.beginTransaction();
            bean.deleteFromRealm();
            realm.commitTransaction();
        }
        return true;
    }
}