package com.aserbao.androidcustomcamera.whole.editVideo.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aserbao.androidcustomcamera.R;
import com.aserbao.androidcustomcamera.base.MyApplication;

import java.util.LinkedHashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * <pre>
 *     author : Administrator (Jacket)
 *     e-mail : 378315764@qq.com
 *     time   : 2018/02/10
 *     desc   :
 *     version: 3.2
 * </pre>
 */

public class PopBubbleEditView {
    @BindView(R.id.ed_content)
    EditText edContent;
    @BindView(R.id.tv_confirm)
    TextView tvConfirm;
    private String TAG = PopBubbleEditView.class.getSimpleName();

    private Context context;
    private PopupWindow popupWindow;
    private View popupWindowView;

    private RelativeLayout rlReleasePornographicContent;

    private RelativeLayout rlIssueViolenceContent;

    private RelativeLayout rlHarass;

    private Button submit;

    private CheckBox cbReleasePornographicContent;

    private CheckBox cbIssueViolenceContent;

    private CheckBox cbHarass;

    private String fkMobile;

    private String clientToken;

    private String fkMobileWasReport;

    private String reportType;

    private String reportDec;

    private String initText;


    public PopBubbleEditView(Context context) {
        this.context = context;
        initPopupWindow();
    }

    /**
     * ?????????
     */
    public void initPopupWindow() {
        if (popupWindowView != null) {
            popupWindow.dismiss();
        }


        popupWindowView = LayoutInflater.from(context).inflate(R.layout.pop_bubble_edit_view, null);
        ButterKnife.bind(this, popupWindowView);
        popupWindow = new PopupWindow(popupWindowView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
//        popupWindow.setAnimationStyle(R.style.popup_window_scale);
        popupWindow.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // ???????????????????????????????????????
//        ColorDrawable dw = new ColorDrawable(0xddffffff);
//        popupWindow.setBackgroundDrawable(dw);
        popupWindow.setOutsideTouchable(true);

        popupWindow.setBackgroundDrawable(new BitmapDrawable());      //?????????????????????back???????????????popupwindow


        // ?????????????????????
        popupWindow.setOnDismissListener(new popupDismissListener());

        popupWindowView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                /*
                 * if( popupWindow!=null && popupWindow.isShowing()){
                 * popupWindow.dismiss(); popupWindow=null; }
                 */
                // ??????????????????true?????????touch??????????????????
                // ????????? PopupWindow???onTouchEvent?????????????????????????????????????????????dismiss
                return false;
            }
        });

        edContent.addTextChangedListener(textWatcher);

    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(s != null && s.toString().length() > 30){
                String tempStr = s.toString().substring(0,30);
                edContent.removeTextChangedListener(textWatcher);
                edContent.setText(tempStr);
                edContent.setSelection(tempStr.length());
                edContent.addTextChangedListener(textWatcher);
                Toast.makeText(context,"????????????????????????30???", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };


    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cb_release_pornographic_content:    //??????????????????
                    Log.e(TAG, " ??????????????????");
                    cbReleasePornographicContent.setChecked(true);
                    cbHarass.setChecked(false);
                    cbIssueViolenceContent.setChecked(false);
                    reportType = "0";
                    break;
                case R.id.cb_issue_violence_content:           //??????????????????
                    Log.e(TAG, "??????????????????");
                    cbIssueViolenceContent.setChecked(true);
                    cbHarass.setChecked(false);
                    cbReleasePornographicContent.setChecked(false);
                    reportType = "1";
                    break;
                case R.id.cb_harass:                             //?????????
                    Log.e(TAG, "?????????");
                    cbHarass.setChecked(true);
                    cbReleasePornographicContent.setChecked(false);
                    cbIssueViolenceContent.setChecked(false);
                    reportType = "2";
                    break;
                case R.id.btn_submit:                            //??????
                    Log.e(TAG, "??????");
                    if (reportType == null || reportType.equals("")) {
                        Toast.makeText(context, "?????????????????????", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    break;
            }
        }
    };


    /**
     * ????????????????????????????????????
     *
     * @param bgAlpha
     */
    public void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = ((Activity) context).getWindow().getAttributes();
        lp.alpha = bgAlpha; // 0.0-1.0
        ((Activity) context).getWindow().setAttributes(lp);
    }

    @OnClick({R.id.ed_content, R.id.tv_confirm})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.ed_content:
                break;
            case R.id.tv_confirm:
                if (edContent.getText().toString().length() > 60) {
                    Toast.makeText(context,"????????????????????????30???", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (onTextSendListener != null) {
                    onTextSendListener.onTextSend(edContent.getText().toString());
                }
                edContent.setText("");
                dimss();
                break;
        }
    }


    class popupDismissListener implements PopupWindow.OnDismissListener {
        @Override
        public void onDismiss() {
            backgroundAlpha(1f);
        }
    }

    public void dimss() {
        if (popupWindow != null) {
            popupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        return popupWindow.isShowing();
    }

    ;

    public void show(String initText) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            if(!initText.equals("??????????????????")){
                this.initText = initText;
                edContent.setText(initText);
                edContent.setSelection(initText.length());
            }
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            //???????????????????????????????????????
            imm.toggleSoftInput(1000, InputMethodManager.HIDE_NOT_ALWAYS);
            //TODO ?????????????????? R.layout.activity_main????????????????????????????????????popupwindow?????????????????????????????????????????????????????????????????????????????????
            popupWindow.showAtLocation(LayoutInflater.from(context).inflate(R.layout.base_activity, null),
                    Gravity.BOTTOM, 0, 0);
        }
    }


    public interface OnTextSendListener{
        void onTextSend(String text);
    }

    public OnTextSendListener onTextSendListener;

    public void setOnTextSendListener(OnTextSendListener onTextSendListener){
        this.onTextSendListener = onTextSendListener;
    }


    private PopTopTipWindow topTipWindow;
    private long last = 0;
    public void showPop(String s,Context mContext){
        long star = System.currentTimeMillis();
        long cha = star - last;
        if(cha/1000 < 5){
            return;
        }
        last = star;
        if(topTipWindow != null && topTipWindow.isShowing()){
            topTipWindow.dimss();
            topTipWindow = null;
        }else {
            topTipWindow = new PopTopTipWindow(mContext, s);
            if (!((Activity) mContext).isFinishing()) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (topTipWindow != null && topTipWindow.isShowing()) {
                            topTipWindow.dimss();
                        }
                    }
                }, 1000);
            }
        }
    }

}
