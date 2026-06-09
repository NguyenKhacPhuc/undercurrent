import SwiftUI
import UIKit
import ComposeApp

/// Native SwiftUI rendering of `PlatformActionButton` — a prominent,
/// system-styled button. This is what iOS shows where Android shows the
/// Material `Button`.
struct ActionButtonView: View {
    let label: String
    let enabled: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text(label).frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(!enabled)
    }
}

/// Bridges the SwiftUI view to Compose. Conforms to the Kotlin-declared
/// `NativeActionButton` protocol so Compose can build it once and re-push
/// `label` / `enabled` on each recomposition via `update`.
final class ActionButtonBridge: NativeActionButton {
    private let onClick: () -> Void
    private var label: String = ""
    private var enabled: Bool = true
    private lazy var hosting = UIHostingController(rootView: makeView())

    init(onClick: @escaping () -> Void) {
        self.onClick = onClick
    }

    private func makeView() -> ActionButtonView {
        ActionButtonView(label: label, enabled: enabled, onClick: onClick)
    }

    var viewController: UIViewController { hosting }

    func update(label: String, enabled: Bool) {
        self.label = label
        self.enabled = enabled
        hosting.rootView = makeView()
        hosting.view.backgroundColor = .clear
    }
}
