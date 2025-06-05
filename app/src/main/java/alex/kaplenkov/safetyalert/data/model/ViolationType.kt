package alex.kaplenkov.safetyalert.data.model

import androidx.annotation.DrawableRes
import alex.kaplenkov.safetyalert.R

enum class ViolationType(val displayName: String, @DrawableRes val iconRes: Int) {
    //SMOKING("Курение в неположенном месте", R.drawable.smoke_icon),
    HANDRAIL("Несоблюдение обязательного использования поручня", R.drawable.stairs_icon),
    HELMET("Нарушение норм по ношению каски", R.drawable.helmet_icon);

    companion object {
        fun fromDisplayName(name: String): ViolationType? {
            return entries.find { it.displayName == name }
        }
    }
}