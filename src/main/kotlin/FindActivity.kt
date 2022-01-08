import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.io.BufferedReader
import java.io.InputStreamReader


class FindActivity : AnAction() {

    /** The path of ADB*/
    private var adbPath = ""

    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project!!

        // Must use absolute path if used jvm command line
        adbPath = AndroidSdkUtils.getAdb(project)?.path.toString()

        val result = execCMD("$adbPath shell dumpsys activity activities | grep mResumedActivity")
        println(result)


        if (result.isBlank()) {
            return showNotification(project, "Please check your phone is properly connected!")
        }
        if (result.contains(".launcher/")) {
            return showNotification(project, "Please open the corresponding app of your project on your mobile phone!")
        }

        val activityPackage = result.split("/")[1].split(" ")[0]
        println(activityPackage)
        val splitResult = activityPackage.split(".")
        val activity = splitResult[splitResult.size - 1]
        println(activity)


        val result2 = execCMD("$adbPath shell dumpsys activity $activityPackage")
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


    /**
     * Show information with Notification
     * @param project Project
     * @param content The content will show
     */
    private fun showNotification(project: Project, content: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("Custom Notification Group")
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }


}