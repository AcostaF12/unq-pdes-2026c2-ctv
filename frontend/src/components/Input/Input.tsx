import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from 'react'
import './Input.css'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  rightElement?: ReactNode
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, id, className, rightElement, ...rest },
  ref,
) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const errorId = `${inputId}-error`

  return (
    <div className="field">
      <label className="field__label" htmlFor={inputId}>
        {label}
      </label>
      <div className="field__input-wrapper">
        <input
          ref={ref}
          id={inputId}
          className={['field__input', rightElement ? 'field__input--with-icon' : '', error ? 'field__input--error' : '', className]
            .filter(Boolean)
            .join(' ')}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          {...rest}
        />
        {rightElement && <div className="field__right-slot">{rightElement}</div>}
      </div>
      {error && (
        <p className="field__error" id={errorId} role="alert">
          {error}
        </p>
      )}
    </div>
  )
})
