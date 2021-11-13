import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.search.GlobalSearchScope
import java.io.BufferedReader
import java.io.InputStreamReader


class FindActivity : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project!!

        // 这里目前是有问题的，如果要是进行适配的话，第一步需要确认adb的路径
        // 在jvm里运行命令必须要带全部路径才可以
        val adbPwd = execCMD("which adb")
        NotificationGroupManager.getInstance().getNotificationGroup("Custom Notification Group")
            .createNotification(adbPwd, NotificationType.ERROR)
            .notify(project)


        val result = execCMD("adb shell dumpsys activity activities | grep mResumedActivity")
        NotificationGroupManager.getInstance().getNotificationGroup("Custom Notification Group")
            .createNotification(result, NotificationType.ERROR)
            .notify(project)
        if (result.isBlank()) {
            NotificationGroupManager.getInstance().getNotificationGroup("Custom Notification Group")
                .createNotification("Please check your phone is properly connected!", NotificationType.ERROR)
                .notify(project)
            return
        }
        if (result.contains(".launcher/")) {
            NotificationGroupManager.getInstance().getNotificationGroup("Custom Notification Group")
                .createNotification(
                    "Please open the corresponding app of your project on your mobile phone!",
                    NotificationType.ERROR
                )
                .notify(project)
            return
        }
        val activityPackage = result.split("/")[1].split(" ")[0]
        println(activityPackage)
        val splitResult = activityPackage.split(".")
        val activity = splitResult[splitResult.size - 1]
        println(activity)

        val result2 = execCMD("adb shell dumpsys activity $activityPackage")
        var flag = 0
        val fragments: MutableList<String?> = ArrayList()
        val lines = result2.split("\n").reversed()
        for (line in lines) {
            if (line.contains("#") && flag == 0) flag++
            if (line.contains("Added")) flag++
            if (flag == 1 && line.trim().startsWith("#")) {
                fragments.add(line.trim().split("{")[0].split(" ")[1])
            }
        }
        fragments.reverse()
        fragments.remove("SupportRequestManagerFragment")
        println(fragments)


        fragments.add(0, activity)


        try {
            val scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
            val chooser = TreeClassChooserFactory.getInstance(project)
                .createNoInnerClassesScopeChooser(
                    "Choose Class to Move",
                    scope,
                    {
                        fragments.contains(it.name)
                    },
                    null
                )
            chooser.showDialog()
            if (chooser.selected != null) NavigationUtil.activateFileWithPsiElement(chooser.selected)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    /**
     * Execute command in terminal
     * @param command String
     * @return String
     */
    private fun execCMD(command: String): String {
        val sb = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec(command)
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        } catch (e: Exception) {
            return e.toString()
        }
        return sb.toString()
    }


}