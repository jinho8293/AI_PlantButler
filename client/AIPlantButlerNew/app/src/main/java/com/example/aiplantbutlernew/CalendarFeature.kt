package com.example.aiplantbutlernew

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 의존성 없이 동작하는 썸네일 캘린더 + 일기 프래그먼트 (모던한 카드형 UI)
 */
class CalendarLegacyFragment : Fragment() {

    private val prefsName = "diary_prefs"
    private val prefPhotosKey = "photo_map"
    private val reqPickImage = 4011

    // 상태
    private var currentMonth: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var selectedDayKey: Long = startOfDayKey(System.currentTimeMillis())

    // UI refs
    private lateinit var tvMonth: TextView
    private lateinit var rvGrid: RecyclerView
    private lateinit var tvSelectedDate: TextView
    private lateinit var etDiary: EditText
    private lateinit var ivPreview: ImageView
    private lateinit var btnPick: Button
    private lateinit var btnClearPhoto: Button
    private lateinit var btnSave: Button

    // 데이터(사진 맵 캐시)
    private val photoMap: MutableMap<Long, Uri> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoMap.clear()
        photoMap.putAll(loadPhotoMap(requireContext()))
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()

        val rootScroll = ScrollView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }
        val page = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        rootScroll.addView(page)

        // ===== 상단 그린 헤더 =====
        val header = LinearLayout(ctx).apply {
            setBackgroundColor(0xFF1DB954.toInt()) // 녹색 톤
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
            )
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val back = ImageButton(ctx).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            background = null
            setColorFilter(Color.WHITE)
            contentDescription = "뒤로"
        }
        val headerTitle = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = "Home"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val calendarIcon = ImageButton(ctx).apply {
            setImageResource(android.R.drawable.ic_menu_my_calendar)
            background = null
            setColorFilter(Color.WHITE)
            contentDescription = "달력"
        }
        header.addView(back)
        header.addView(headerTitle)
        header.addView(calendarIcon)
        page.addView(header)

        // ===== 카드 컨테이너 (흰 배경, 상단 모서리 둥글게 느낌은 margin으로 대체) =====
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
                leftMargin = dp(12)
                rightMargin = dp(12)
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.WHITE)
        }
        page.addView(card)

        // 제목
        val title = TextView(ctx).apply {
            text = "Calendar"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }
        card.addView(title)

        // 요일 헤더
        val dowRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        val dows = listOf("Sur","Mo","My","Th","Fr","Stt","Su") // 샘플 표기 스타일
        dows.forEach { s ->
            val t = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                setTextColor(0xFF8A8A8A.toInt())
                text = s
            }
            dowRow.addView(t)
        }
        card.addView(dowRow)

        // 헤더(이전/월/다음)
        val monthCtl = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val btnPrev = ImageButton(ctx).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            background = null
            contentDescription = "이전 달"
        }
        tvMonth = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        }
        val btnNext = ImageButton(ctx).apply {
            setImageResource(android.R.drawable.ic_media_next)
            background = null
            contentDescription = "다음 달"
        }
        monthCtl.addView(btnPrev)
        monthCtl.addView(tvMonth)
        monthCtl.addView(btnNext)
        card.addView(monthCtl)

        // 캘린더 그리드 (얇은 그리드라인 효과를 위해 cell margin 사용)
        rvGrid = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(360)
            ).apply { topMargin = dp(6) }
            layoutManager = GridLayoutManager(ctx, 7)
            isNestedScrollingEnabled = false
            setBackgroundColor(0xFFEFEFEF.toInt()) // 라인 느낌
        }
        card.addView(rvGrid)

        // 선택 영역 (미리보기/일기)
        tvSelectedDate = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            setTypeface(typeface, Typeface.BOLD)
        }
        card.addView(tvSelectedDate)

        ivPreview = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(150)
            ).apply { topMargin = dp(6) }
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "사진 미리보기"
        }
        card.addView(ivPreview)

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        btnPick = Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = "사진 선택"
        }
        btnClearPhoto = Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(8)
            }
            text = "사진 삭제"
        }
        btnRow.addView(btnPick)
        btnRow.addView(btnClearPhoto)
        card.addView(btnRow)

        etDiary = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(110)
            ).apply { topMargin = dp(8) }
            gravity = Gravity.TOP or Gravity.START
            hint = "일기를 입력하세요"
        }
        card.addView(etDiary)

        btnSave = Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            text = "저장"
        }
        card.addView(btnSave)

        // ===== 동작 바인딩 =====
        fun refreshMonthUI() {
            tvMonth.text = SimpleDateFormat("yyyy년 M월", Locale.getDefault()).format(currentMonth.time)
            rvGrid.adapter = MonthAdapter(buildDaysOfMonth(currentMonth), selectedDayKey)
        }
        btnPrev.setOnClickListener { currentMonth.add(Calendar.MONTH, -1); refreshMonthUI() }
        btnNext.setOnClickListener { currentMonth.add(Calendar.MONTH, 1); refreshMonthUI() }

        btnPick.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, reqPickImage)
        }
        btnClearPhoto.setOnClickListener {
            savePhotoUri(requireContext(), selectedDayKey, null)
            photoMap.remove(selectedDayKey)
            ivPreview.setImageDrawable(null)
            rvGrid.adapter?.notifyDataSetChanged()
        }
        btnSave.setOnClickListener {
            saveDiaryText(requireContext(), selectedDayKey, etDiary.text?.toString() ?: "")
            Toast.makeText(requireContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 초기
        setSelectedDate(selectedDayKey)
        refreshMonthUI()

        return rootScroll
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqPickImage && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            savePhotoUri(requireContext(), selectedDayKey, uri)
            photoMap[selectedDayKey] = uri
            refreshForSelectedDay()
            rvGrid.adapter?.notifyDataSetChanged()
        }
    }

    private fun setSelectedDate(dayKey: Long) {
        selectedDayKey = dayKey
        tvSelectedDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayKey)
        refreshForSelectedDay()
        rvGrid.adapter = MonthAdapter(buildDaysOfMonth(currentMonth), selectedDayKey)
    }

    private fun refreshForSelectedDay() {
        etDiary.setText(loadDiaryText(requireContext(), selectedDayKey))
        val uri = photoMap[selectedDayKey] ?: loadPhotoUri(requireContext(), selectedDayKey)?.also {
            photoMap[selectedDayKey] = it
        }
        if (uri != null) {
            try {
                requireContext().contentResolver.openInputStream(uri).use { ins ->
                    val bmp = if (ins != null) BitmapFactory.decodeStream(ins) else null
                    if (bmp != null) ivPreview.setImageBitmap(bmp) else ivPreview.setImageDrawable(null)
                }
            } catch (_: Exception) { ivPreview.setImageDrawable(null) }
        } else ivPreview.setImageDrawable(null)
    }

    private fun buildDaysOfMonth(monthCal: Calendar): List<Long?> {
        val temp = (monthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val leading = temp.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<Long?>()
        repeat(leading) { list += null }
        repeat(daysInMonth) { d ->
            val c = temp.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, d + 1)
            list += c.timeInMillis
        }
        while (list.size % 7 != 0) list += null
        return list
    }

    // 저장/로딩
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private fun diaryTextKey(dayKey: Long) = "text_$dayKey"
    private fun saveDiaryText(ctx: Context, dayKey: Long, text: String) { prefs(ctx).edit().putString(diaryTextKey(dayKey), text).apply() }
    private fun loadDiaryText(ctx: Context, dayKey: Long): String = prefs(ctx).getString(diaryTextKey(dayKey), "") ?: ""
    private fun savePhotoUri(ctx: Context, dayKey: Long, uri: Uri?) {
        val set = (prefs(ctx).getStringSet(prefPhotosKey, emptySet()) ?: emptySet()).toMutableSet()
        val prefix = "$dayKey="
        set.removeAll { it.startsWith(prefix) }
        if (uri != null) set.add(prefix + uri.toString())
        prefs(ctx).edit().putStringSet(prefPhotosKey, set).apply()
    }
    private fun loadPhotoUri(ctx: Context, dayKey: Long): Uri? {
        val set = prefs(ctx).getStringSet(prefPhotosKey, emptySet()) ?: emptySet()
        val prefix = "$dayKey="
        val raw = set.firstOrNull { it.startsWith(prefix) } ?: return null
        return try { Uri.parse(raw.substringAfter('=')) } catch (_: Exception) { null }
    }
    private fun loadPhotoMap(ctx: Context): Map<Long, Uri> {
        val set = prefs(ctx).getStringSet(prefPhotosKey, emptySet()) ?: emptySet()
        val result = mutableMapOf<Long, Uri>()
        for (s in set) {
            val idx = s.indexOf('=')
            if (idx > 0) {
                val key = s.substring(0, idx).toLongOrNull() ?: continue
                val uri = runCatching { Uri.parse(s.substring(idx + 1)) }.getOrNull() ?: continue
                result[key] = uri
            }
        }
        return result
    }

    private fun startOfDayKey(millis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }
    private fun dp(value: Int): Int = (resources.displayMetrics.density * value).toInt()

    // 7열 캘린더 셀 어댑터 (셀 사이 간격을 margin으로 만들어 라인처럼 보이게)
    private inner class MonthAdapter(
        private val items: List<Long?>,
        private val selectedKey: Long
    ) : RecyclerView.Adapter<MonthAdapter.VH>() {

        inner class VH(val root: LinearLayout, val tv: TextView, val iv: ImageView) :
            RecyclerView.ViewHolder(root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val cell = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(86))
                setPadding(dp(2), dp(2), dp(2), dp(2))
                setBackgroundColor(Color.WHITE)
            }
            // 간격(그리드 라인 효과)
            (cell.layoutParams as ViewGroup.LayoutParams).let {
                if (it is RecyclerView.LayoutParams) {
                    it.setMargins(dp(1), dp(1), dp(1), dp(1))
                }
            }
            val tv = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.START
                textSize = 12f
                setTextColor(0xFF333333.toInt())
            }
            val iv = ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(56)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            cell.addView(tv)
            cell.addView(iv)
            return VH(cell, tv, iv)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val millis = items[position]
            if (millis == null) {
                holder.tv.text = ""
                holder.iv.setImageDrawable(null)
                holder.root.setBackgroundColor(Color.WHITE)
                holder.root.setOnClickListener(null)
                return
            }
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            holder.tv.text = day.toString()
            when (dow) {
                Calendar.SUNDAY -> holder.tv.setTextColor(0xFFD32F2F.toInt())
                Calendar.SATURDAY -> holder.tv.setTextColor(0xFF1976D2.toInt())
                else -> holder.tv.setTextColor(0xFF333333.toInt())
            }
            val key = startOfDayKey(millis)
            val uri = photoMap[key]
            if (uri != null) {
                try {
                    holder.itemView.context.contentResolver.openInputStream(uri).use { ins ->
                        val bmp = if (ins != null) BitmapFactory.decodeStream(ins) else null
                        if (bmp != null) holder.iv.setImageBitmap(bmp) else holder.iv.setImageDrawable(null)
                    }
                } catch (_: Exception) {
                    holder.iv.setImageDrawable(null)
                }
            } else holder.iv.setImageDrawable(null)

            val todayKey = startOfDayKey(System.currentTimeMillis())
            when (key) {
                selectedKey -> holder.root.setBackgroundColor(0xFFE8F5E9.toInt())
                todayKey -> holder.root.setBackgroundColor(0xFFF7F7F7.toInt())
                else -> holder.root.setBackgroundColor(Color.WHITE)
            }
            holder.root.setOnClickListener { setSelectedDate(key) }
        }
    }
}