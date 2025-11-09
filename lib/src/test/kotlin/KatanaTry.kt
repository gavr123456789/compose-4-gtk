import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.compose4gtk.adw.adwApplication
import io.github.compose4gtk.adw.components.ActionRow
import io.github.compose4gtk.adw.components.ApplicationWindow
import io.github.compose4gtk.adw.components.ButtonRow
import io.github.compose4gtk.adw.components.Carousel
import io.github.compose4gtk.adw.components.CarouselIndicatorDots
import io.github.compose4gtk.adw.components.CarouselIndicatorLines
import io.github.compose4gtk.adw.components.ComboRow
import io.github.compose4gtk.adw.components.EntryRow
import io.github.compose4gtk.adw.components.HeaderBar
import io.github.compose4gtk.adw.components.PreferencesGroup
import io.github.compose4gtk.adw.components.SpinRow
import io.github.compose4gtk.adw.components.StatusPage
import io.github.compose4gtk.adw.components.SwitchRow
import io.github.compose4gtk.adw.components.ToastOverlay
import io.github.compose4gtk.adw.components.rememberCarouselState
import io.github.compose4gtk.gtk.ImageSource
import io.github.compose4gtk.gtk.components.Box
import io.github.compose4gtk.gtk.components.Button
import io.github.compose4gtk.gtk.components.IconButton
import io.github.compose4gtk.gtk.components.Label
import io.github.compose4gtk.gtk.components.ListBox
import io.github.compose4gtk.gtk.components.ScrolledWindow
import io.github.compose4gtk.gtk.components.ToggleButton
import io.github.compose4gtk.gtk.components.VerticalBox
import io.github.compose4gtk.gtk.components.rememberSelectionModel
import io.github.compose4gtk.modifier.Modifier
import io.github.compose4gtk.modifier.alignment
import io.github.compose4gtk.modifier.combine
import io.github.compose4gtk.modifier.cssClasses
import io.github.compose4gtk.modifier.expand
import io.github.compose4gtk.modifier.expandHorizontally
import io.github.compose4gtk.modifier.expandVertically
import io.github.compose4gtk.modifier.margin
import io.github.compose4gtk.modifier.sizeRequest
import io.github.compose4gtk.useGioResource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import io.github.oshai.kotlinlogging.KotlinLogging
import org.gnome.adw.ColorScheme
import org.gnome.adw.StyleManager
import org.gnome.adw.Toast
import org.gnome.gtk.Align
import org.gnome.gtk.Orientation
import org.gnome.gtk.PolicyType
import org.gnome.gtk.SelectionMode
import kotlin.io.path.extension
import io.github.compose4gtk.gtk.components.SelectionMode as ListSelectionMode

private val logger = KotlinLogging.logger {}

// Usefull CSS classes: "card", "boxed-list"(like Preference Group)

fun main(args: Array<String>) {
    useGioResource("resources.gresource") {
        adwApplication("my.example.hello-app", args) {
            ApplicationWindow("Preferences", onClose = ::exitApplication, defaultWidth = 800, defaultHeight = 600) {
                VerticalBox(Modifier.expand()) {

                    HeaderBar(modifier = Modifier.cssClasses("flat"), title = { Label("KaTana") })
                    FinderCarousel()

                }
            }
        }
    }
}


@Composable
private fun Settings(
    allowLongSwipes: MutableState<Boolean>,
    allowMouseDrag: MutableState<Boolean>,
    allowScrollWheel: MutableState<Boolean>,
) {
    StatusPage(title = "Settings") {
        Box(orientation = Orientation.VERTICAL, modifier = Modifier.margin(16), spacing = 16) {
            Label("It can be modified")
            ToggleButton(
                label = "Allow long swipes",
                active = allowLongSwipes.value,
                onToggle = {
                    allowLongSwipes.value = !allowLongSwipes.value
                },
            )
            ToggleButton(
                label = "Allow mouse drag",
                active = allowMouseDrag.value,
                onToggle = {
                    allowMouseDrag.value = !allowMouseDrag.value
                },
            )
            ToggleButton(
                label = "Allow scroll wheel",
                active = allowScrollWheel.value,
                onToggle = {
                    allowScrollWheel.value = !allowScrollWheel.value
                },
            )
        }
    }
}

@Composable
private fun MoreSettings(orientation: MutableState<Orientation>) {
    StatusPage(title = "More Settings!") {
        Box(orientation = Orientation.VERTICAL, modifier = Modifier.margin(16), spacing = 16) {
            Label("I am ${if (orientation.value == Orientation.HORIZONTAL) "Horizontal" else "Vertical"}")
            Button(label = "Change orientation", onClick = {
                if (orientation.value == Orientation.HORIZONTAL) {
                    orientation.value = Orientation.VERTICAL
                } else {
                    orientation.value = Orientation.HORIZONTAL
                }
            })
        }
    }
}


val maxNameLength = 20

@Composable
private fun DirectoryColumn(path: Path, onOpen: (Path) -> Unit) {
    val entries = remember(path) {
        val items = mutableListOf<Path>()
        try {
            Files.newDirectoryStream(path).use { ds ->
                ds.forEach { p ->
                    // Простейшая фильтрация скрытых (unix-подобные)
                    val name = p.fileName?.toString() ?: ""
                    if (!name.startsWith("."))
                        items.add(p)
                }
            }
        } catch (t: Throwable) {
            logger.error(t) { "Failed to list directory: $path" }
        }

        items.sortedWith(
            compareBy<Path>({ !Files.isDirectory(it) }, { it.fileName?.toString()?.lowercase() ?: "" })
        )
    }

    VerticalBox (
        modifier = Modifier.expand(),
//        modifier = Modifier.cssClasses("card"),
    ) {

        Label(path.last().toString(), modifier = Modifier.margin(8))
        ScrolledWindow(Modifier.expand(true), horizontalScrollbarPolicy = PolicyType.NEVER) { // Modifier.expand(), propagateNaturalWidth = true, propagateNaturalHeight = true
            ListBox(
                modifier = Modifier.cssClasses("boxed-list"),
                selectionMode = SelectionMode.SINGLE,
                activateOnSingleClick = true) {
                entries.forEach { p ->
                    val isDir = Files.isDirectory(p)
                    ActionRow(
                        modifier = Modifier.sizeRequest(width = 240, height = 48),
                        title = p.toFile().nameWithoutExtension.let {
                            if (it.length > maxNameLength) it.take(maxNameLength - 3) + "..." else it
                        },
                        subtitle = p.extension,
                        titleLines = 1,
//                        titleSelectable = true,
                        prefix = {
                            IconButton(
                                icon = ImageSource.Icon(if (isDir) "folder-symbolic" else "text-x-generic-symbolic"),
                                onClick = { onOpen(p) },
                                modifier = Modifier.margin(0, 6).sizeRequest(38, 38)
                            )
                        },
                        onActivate = { onOpen(p) },
//                        suffix = {
//                            Label(p.toFile().nameWithoutExtension, modifier = Modifier.expandHorizontally().alignment(horizontal = Align.START))
//                        }

                    )
                }
            }

        }
    }
}

@Composable
fun FinderCarousel(root: Path = Paths.get(System.getProperty("user.home"))) {

    var pagesPaths by remember { mutableStateOf(listOf(root)) }
    val orientation = remember { mutableStateOf(Orientation.HORIZONTAL) }
    val carouselState = rememberCarouselState(pagesPaths.size + 1, orientation.value)

    LaunchedEffect(1) {
        carouselState.scrollTo(1, animate = false)
    }

    Box(
        orientation = Orientation.VERTICAL, //if (orientation.value == Orientation.HORIZONTAL) Orientation.VERTICAL else Orientation.HORIZONTAL,
        modifier = Modifier
            .margin(8)
            .alignment(horizontal = Align.FILL)//.expand()

    ) {
        Carousel(
            state = carouselState,
            modifier = Modifier.expand(),
            spacing = 24,
            allowLongSwipes = true,
        ) { page ->
            val currentPath = pagesPaths.getOrNull(page)
            if (currentPath != null) {
                DirectoryColumn( path = currentPath, onOpen = { target ->
                    if (Files.isDirectory(target)) {
                        val base = pagesPaths.take(page + 1)
                        pagesPaths = buildList {
                            addAll(base)
                            add(target)
                        }
                        // scroll to the last page
                        val scrTo = pagesPaths.count() - 1
                        carouselState.scrollTo(scrTo, animate = true)
                    } else {
                        logger.info { "Selected file: $target" }
                    }
                })

            } else {
                Label("")
            }

        }
        CarouselIndicatorDots(carouselState)
        CarouselIndicatorLines(carouselState)
    }
}

