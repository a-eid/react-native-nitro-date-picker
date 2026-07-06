import NitroModules
import UIKit

class HybridNitroDatePicker: HybridNitroDatePickerSpec {
  // MARK: - Properties
  var onDateChange: ((Double) -> Void)?
  var onConfirm: ((Double) -> Void)?
  var onCancel: (() -> Void)?

  private var alertController: UIAlertController?
  private var datePicker: UIDatePicker?
  private var currentConfig: DatePickerConfig?

  // MARK: - Show

  func show(config: DatePickerConfig) throws {
    currentConfig = config

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
    pickerViewController.preferredContentSize = pickerSize(for: config.mode)

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

    applyTextColor(to: picker, color: config.textColor)

    picker.addTarget(self, action: #selector(dateChanged(_:)), for: .valueChanged)

    return picker
  }

  @objc private func dateChanged(_ sender: UIDatePicker) {
    let timestamp = sender.date.timeIntervalSince1970 * 1000
    onDateChange?(timestamp)
  }

  private func applyTextColor(to picker: UIDatePicker, color: String?) {
    guard let hex = color else { return }
    if hex.lowercased() == "#000000" {
      picker.overrideUserInterfaceStyle = .light
    } else if hex.lowercased() == "#ffffff" {
      picker.overrideUserInterfaceStyle = .dark
    }
  }

  private func applyTheme(to alert: UIAlertController, theme: DatePickerTheme?) {
    switch theme {
    case .light:
      alert.overrideUserInterfaceStyle = .light
    case .dark:
      alert.overrideUserInterfaceStyle = .dark
    default:
      break
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

  private func pickerSize(for mode: DatePickerMode) -> CGSize {
    let width: CGFloat = UIScreen.main.bounds.width > 400 ? 320 : UIScreen.main.bounds.width - 30
    let height: CGFloat = 216
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
