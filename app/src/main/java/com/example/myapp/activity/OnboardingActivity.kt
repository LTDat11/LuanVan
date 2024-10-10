package com.example.myapp.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.R
import com.example.myapp.adapter.MyViewPagerOnboardingAdapter
import me.relex.circleindicator.CircleIndicator3

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicator: CircleIndicator3
    private lateinit var layoutNext: View
    private lateinit var skipButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding)

        // Tham chiếu ViewPager2
        viewPager = findViewById(R.id.view_pager_on_boarding)
        val adapter = MyViewPagerOnboardingAdapter(this)
        viewPager.adapter = adapter

        // Tham chiếu CircleIndicator3 và kết nối với ViewPager2
        indicator = findViewById(R.id.circle_indicator)
        indicator.setViewPager(viewPager)

        // Tham chiếu layout_next và nút Skip
        layoutNext = findViewById(R.id.layout_next)
        skipButton = findViewById(R.id.text_view_skip)

        // Thay đổi sự kiện click cho layout_next
        layoutNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < adapter.itemCount - 1) {
                // Chuyển đến trang tiếp theo
                viewPager.currentItem = currentItem + 1
            }
        }

        // Thay đổi sự kiện click cho nút Skip
        skipButton.setOnClickListener {
            viewPager.currentItem = adapter.itemCount - 1
        }

        // Thêm callback để lắng nghe sự thay đổi trang
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Kiểm tra nếu đang ở trang cuối cùng
                if (position == adapter.itemCount - 1) {
                    // Ẩn layout bottom và nút skip
                    layoutNext.visibility = View.GONE
                    skipButton.visibility = View.GONE
                } else {
                    // Hiện lại layout bottom và nút skip
                    layoutNext.visibility = View.VISIBLE
                    skipButton.visibility = View.VISIBLE
                }
            }
        })
    }
}
