import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TranslateService } from '../../services/translate.service';
import { AVAILABLE_AVATARS, getAvatarPath, type AvatarType } from '../../utils/avatar.util';

@Component({
  selector: 'app-avatar-selector',
  standalone: true,
  imports: [CommonModule, ButtonModule, DialogModule],
  templateUrl: './avatar-selector.component.html',
  styleUrls: ['./avatar-selector.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvatarSelectorComponent implements OnChanges {
  readonly translate = inject(TranslateService);

  @Input() visible = false;
  @Input() currentAvatar: number | null = 1;
  @Input() isLoading = false;
  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<number>();

  readonly availableAvatars = AVAILABLE_AVATARS;
  selectedAvatar: number | null = null;

  ngOnChanges(): void {
    if (this.visible) {
      this.selectedAvatar = this.currentAvatar ?? 1;
    }
  }

  onAvatarSelect(avatarType: AvatarType): void {
    this.selectedAvatar = avatarType;
  }

  onSave(): void {
    if (this.selectedAvatar !== null) {
      this.save.emit(this.selectedAvatar);
    }
  }

  onClose(): void {
    this.close.emit();
    this.selectedAvatar = this.currentAvatar ?? 1;
  }

  getAvatarPath(avatarType: number): string {
    return getAvatarPath(avatarType);
  }
}

