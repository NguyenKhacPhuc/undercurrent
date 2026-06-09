import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        InitKoinKt.doInitKoin()
        // Wire the native SwiftUI rendering of PlatformActionButton. Kotlin
        // can't see Swift types, so the app registers the factory here; the
        // iOS `actual` falls back to a Compose pill if this is ever absent.
        NativeViewRegistry.shared.actionButtonFactory = { onClick in
            // Kotlin `() -> Unit` surfaces to Swift as `() -> KotlinUnit`;
            // discard the returned Unit when adapting to `() -> Void`.
            ActionButtonBridge(onClick: { _ = onClick() })
        }
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
                .ignoresSafeArea(.keyboard)
        }
    }
}
