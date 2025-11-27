import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { ButtonSoundService } from './services/button-sound.service';
import { render, screen } from '@testing-library/angular';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let buttonSoundService: jest.Mocked<ButtonSoundService>;

  beforeEach(async () => {
    buttonSoundService = {
      register: jest.fn(),
      unregister: jest.fn(),
    } as unknown as jest.Mocked<ButtonSoundService>;

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        { provide: ButtonSoundService, useValue: buttonSoundService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should register button sound service on init', () => {
    expect(buttonSoundService.register).toHaveBeenCalled();
  });

  it('should unregister button sound service on destroy', () => {
    component.ngOnDestroy();
    expect(buttonSoundService.unregister).toHaveBeenCalled();
  });

  it('should render router outlet', () => {
    fixture.detectChanges();
    const routerOutlet = fixture.nativeElement.querySelector('router-outlet');
    expect(routerOutlet).toBeTruthy();
  });
});
