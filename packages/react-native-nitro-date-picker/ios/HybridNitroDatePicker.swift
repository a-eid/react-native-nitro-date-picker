import NitroModules
import UIKit

class HybridNitroDatePicker: HybridNitroDatePickerSpec {
  // MARK: - Properties
  var onDateChange: ((Double) -> Void)?
  var onConfirm: ((Double) -> Void)?
  var onCancel: (() -> Void)?

  private var alertController: UIAlertController?
  private var datePicker: UIDatePicker?

  // MARK: - Show

  func show(config: DatePickerConfig) throws {
    DispatchQueue.main.async { [weak self] in
      guard let self = self else { return }
      self.presentPicker(config: config)
    }
  }

  // MARK: - Dismiss

  func dismiss() throws {
    DispatchQueue.main.async { [weak self] in
      self?.alertController?.dismiss(animated: true)
      self?.alertController = nil
      self?.datePicker = nil
    }
  }

  // MARK: - Private Helpers

  private func presentPicker(config: DatePickerConfig) {
    let picker = createDatePicker(config: config)
    self.datePicker = picker

    let alert = UIAlertController(title: config.title ?? defaultTitle(for: config.mode),
                                   message: nil,
                                   preferredStyle: .actionSheet)

    applyTheme(to: alert, theme: config.theme)

    let pickerViewController = UIViewController()
    pickerViewController.view = picker
    pickerViewController.preferredContentSize = pickerSize(for: config)

    // Only apply a custom background when the user explicitly sets style.backgroundColor.
    // For light/dark/auto themes we let the system (via overrideUserInterfaceStyle) handle all
    // colors — forcing a hardcoded fill on top of the system chrome causes mismatched shades.
    if let style = config.style, let hex = style.backgroundColor, let color = UIColor(hex: hex) {
      pickerViewController.view.backgroundColor = color
    }

    alert.setValue(pickerViewController, forKey: "contentViewController")

    let confirmText = config.confirmText ?? "Confirm"
    let confirmAction = UIAlertAction(title: confirmText, style: .default) { [weak self] _ in
      guard let self = self else { return }
      let timestamp = picker.date.timeIntervalSince1970 * 1000
      self.onConfirm?(timestamp)
      self.alertController = nil
      self.datePicker = nil
    }
    alert.addAction(confirmAction)

    let cancelText = config.cancelText ?? "Cancel"
    let cancelAction = UIAlertAction(title: cancelText, style: .cancel) { [weak self] _ in
      self?.onCancel?()
      self?.alertController = nil
      self?.datePicker = nil
    }
    alert.addAction(cancelAction)

    if let popover = alert.popoverPresentationController {
      popover.sourceView = topViewController()?.view
      popover.sourceRect = CGRect(x: UIScreen.main.bounds.midX,
                                   y: UIScreen.main.bounds.midY,
                                   width: 0, height: 0)
      popover.permittedArrowDirections = []
    }

    self.alertController = alert
    topViewController()?.present(alert, animated: true)
  }

  private func createDatePicker(config: DatePickerConfig) -> UIDatePicker {
    let picker = UIDatePicker()
    picker.datePickerMode = uiDatePickerMode(for: config.mode)
    picker.preferredDatePickerStyle = .wheels

    let date = Date(timeIntervalSince1970: config.date / 1000)
    picker.date = date

    if let min = config.minimumDate {
      picker.minimumDate = Date(timeIntervalSince1970: min / 1000)
    }
    if let max = config.maximumDate {
      picker.maximumDate = Date(timeIntervalSince1970: max / 1000)
    }

    picker.minuteInterval = Int(config.minuteInterval)

    if let localeStr = config.locale {
      picker.locale = Locale(identifier: localeStr)
    }

    if let offset = config.timeZoneOffsetInMinutes {
      picker.timeZone = TimeZone(secondsFromGMT: Int(offset) * 60)
    }

    applyTextColor(to: picker, color: effectiveTextColor(config))

    picker.addTarget(self, action: #selector(dateChanged(_:)), for: .valueChanged)

    return picker
  }

  @objc private func dateChanged(_ sender: UIDatePicker) {
    let timestamp = sender.date.timeIntervalSince1970 * 1000
    onDateChange?(timestamp)
  }

  private func applyTextColor(to picker: UIDatePicker, color: String?) {
    guard let hex = color, let uiColor = UIColor(hex: hex) else {
      // No (or unparseable) color: fall back to the system style so the picker stays legible.
      return
    }
    // UIDatePicker does not expose a `textColor` property publicly, but it responds to the KVC key
    // on its underlying labels. This is the same technique henninghall uses, and the only way to
    // apply an arbitrary color to the `.wheels` style on iOS.
    picker.setValue(uiColor, forKey: "textColor")
    picker.setValue(uiColor, forKey: "highlightColor")
  }

  /// The effective interface style for the alert + picker. `.auto` defers to the system, so it
  /// returns `nil` (no override) and lets the trait collection decide.
  private func effectiveUserInterfaceStyle(for theme: DatePickerTheme?) -> UIUserInterfaceStyle? {
    switch theme {
    case .light: return .light
    case .dark: return .dark
    default: return nil
    }
  }

  /// Resolve text color: `style.textColor` ?? theme default. nil for `.auto` (system decides).
  private func effectiveTextColor(_ config: DatePickerConfig) -> String? {
    if let style = config.style, let hex = style.textColor { return hex }
    switch config.theme {
    case .light: return "#000000"
    case .dark: return "#ffffff"
    // .auto / unset: let UIDatePicker follow the system; no KVC override.
    default: return nil
    }
  }

  private func applyTheme(to alert: UIAlertController, theme: DatePickerTheme?) {
    // Apply the override to both the alert and its presented content so the `.wheels` picker and
    // the action-sheet chrome stay consistent. `.auto` leaves the system in charge.
    if let style = effectiveUserInterfaceStyle(for: theme) {
      alert.overrideUserInterfaceStyle = style
    }
  }

  private func uiDatePickerMode(for mode: DatePickerMode) -> UIDatePicker.Mode {
    switch mode {
    case .date: return .date
    case .time: return .time
    case .datetime: return .dateAndTime
    }
  }

  private func defaultTitle(for mode: DatePickerMode) -> String {
    switch mode {
    case .time: return "Select time"
    default: return "Select date"
    }
  }

  private func pickerSize(for config: DatePickerConfig) -> CGSize {
    let width: CGFloat = UIScreen.main.bounds.width > 400 ? 320 : UIScreen.main.bounds.width - 30
    // style.height is in dp; on iOS dp ≈ pt (1:1 on non-retina, scaled uniformly), so we use it
    // directly. Falls back to the system default wheel height of 216pt.
    let height: CGFloat = config.style?.height.map { CGFloat($0) } ?? 216
    return CGSize(width: width, height: height)
  }

  private func topViewController() -> UIViewController? {
    guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
          let rootVC = windowScene.windows.first?.rootViewController else {
      return nil
    }
    var top = rootVC
    while let presented = top.presentedViewController {
      top = presented
    }
    return top
  }
}

// MARK: - UIColor hex parsing

private extension UIColor {
  /// Parses a CSS-style hex color (`#rgb`, `#rrggbb`, `#aarrggbb`). Returns nil on malformed input.
  convenience init?(hex: String) {
    var cleaned = hex.trimmingCharacters(in: .whitespacesAndNewlines)
    if cleaned.hasPrefix("#") { cleaned.removeFirst() }

    guard let value = UInt32(cleaned, radix: 16) else { return nil }

    let r: CGFloat
    let g: CGFloat
    let b: CGFloat
    let a: CGFloat

    switch cleaned.count {
    case 3: // #rgb → #rrggbb
      r = CGFloat((value >> 8) & 0xF) / 15
      g = CGFloat((value >> 4) & 0xF) / 15
      b = CGFloat(value & 0xF) / 15
      a = 1
    case 6: // #rrggbb
      r = CGFloat((value >> 16) & 0xFF) / 255
      g = CGFloat((value >> 8) & 0xFF) / 255
      b = CGFloat(value & 0xFF) / 255
      a = 1
    case 8: // #aarrggbb
      a = CGFloat((value >> 24) & 0xFF) / 255
      r = CGFloat((value >> 16) & 0xFF) / 255
      g = CGFloat((value >> 8) & 0xFF) / 255
      b = CGFloat(value & 0xFF) / 255
    default:
      return nil
    }

    self.init(red: r, green: g, blue: b, alpha: a)
  }
}
