import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-edit-username-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputTextModule, ButtonModule, DialogModule],
  templateUrl: './edit-username-dialog.component.html',
  styleUrls: ['./edit-username-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditUsernameDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly translate = inject(TranslateService);

  @Input() currentUsername: string = '';
  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<string>();

  editForm!: FormGroup;
  visible = true;

  ngOnInit(): void {
    this.editForm = this.fb.group({
      username: [
        this.currentUsername,
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(50),
          Validators.pattern(/^[a-zA-Z0-9_-]+$/),
        ],
      ],
    });
  }

  onSave(): void {
    if (this.editForm.valid) {
      const newUsername = this.editForm.get('username')?.value?.trim();
      if (newUsername) {
        this.save.emit(newUsername);
      }
    } else {
      this.editForm.markAllAsTouched();
    }
  }

  onClose(): void {
    this.visible = false;
    this.close.emit();
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.editForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.editForm.get(fieldName);
    if (!field || !field.errors) {
      return '';
    }

    if (field.errors['required']) {
      return this.translate.translate('profile.edit.username.required');
    }
    if (field.errors['minlength']) {
      return this.translate.translate('profile.edit.username.minLength');
    }
    if (field.errors['maxlength']) {
      return this.translate.translate('profile.edit.username.maxLength');
    }
    if (field.errors['pattern']) {
      return this.translate.translate('profile.edit.username.pattern');
    }

    return '';
  }
}

