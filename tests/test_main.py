"""
src/main.py 에 대한 기본 테스트
"""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from main import main


def test_main_runs():
    """main() 함수가 오류 없이 실행되는지 확인"""
    main()  # 예외가 발생하지 않으면 통과
