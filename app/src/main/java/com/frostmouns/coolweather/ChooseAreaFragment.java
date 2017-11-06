package com.frostmouns.coolweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.frostmouns.coolweather.db.City;
import com.frostmouns.coolweather.db.County;
import com.frostmouns.coolweather.db.Province;
import com.frostmouns.coolweather.util.HttpUtil;
import com.frostmouns.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by frostmouns on 2017/11/6.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private static final String TAG = "HHHH";

    private ProgressDialog mProgressDialog;

    private TextView mTvTitle;
    private Button mBtnBack;
    private ListView mLvIteam;

    private ArrayAdapter<String> mAdapter;
    private List<String> mDataList = new ArrayList<>();

    private List<Province> mProvinceList;
    private List<City> mCityList;
    private List<County> mCountyList;

    private Province selectdProvince;
    private City selectedCity;
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        mTvTitle = view.findViewById(R.id.tv_title);
        mBtnBack = view.findViewById(R.id.btn_back);
        mLvIteam = view.findViewById(R.id.lv_iteam);

        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mDataList);
        mLvIteam.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLvIteam.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long ld) {
                if(currentLevel == LEVEL_PROVINCE ){
                    selectdProvince = mProvinceList.get(position);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectedCity = mCityList.get(position);
                    queryCounties();
                }
            }
        });

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        mTvTitle.setText("中国");
        mBtnBack.setVisibility(View.GONE);
        mProvinceList = DataSupport.findAll(Province.class);
        if(mProvinceList.size() > 0) {
            mDataList.clear();
            for (Province province : mProvinceList) {
                mDataList.add(province.getName());
            }
            mAdapter.notifyDataSetChanged();
            mLvIteam.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    private void queryCities() {
        mTvTitle.setText(selectdProvince.getName());
        mBtnBack.setVisibility(View.VISIBLE);
        mCityList = DataSupport.where("provinceid = ?", String.valueOf(selectdProvince.getId())).find(City.class);
        if(mCityList.size() > 0){
            mDataList.clear();
            for (City city:mCityList) {
                mDataList.add(city.getName());
            }
            mAdapter.notifyDataSetChanged();
            mLvIteam.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectdProvince.getCode();
            Log.i(TAG, "provinceCode: " + provinceCode);
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCounties() {
        mTvTitle.setText(selectedCity.getName());
        mBtnBack.setVisibility(View.VISIBLE);
        mCountyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if(mCountyList.size() > 0){
            mDataList.clear();
            for (County county:mCountyList) {
                mDataList.add(county.getName());
            }
            mAdapter.notifyDataSetChanged();
            mLvIteam.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            int provinceCode = selectdProvince.getCode();
            int cityCode = selectedCity.getCode();
            Log.i(TAG, "provinceCode: "+ provinceCode);
            Log.i(TAG, "cityCode: "+ cityCode);
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText, selectdProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog() {
        if(mProgressDialog == null){
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    private void closeProgressDialog() {
        if(mProgressDialog != null){
            mProgressDialog.dismiss();
        }
    }


}
